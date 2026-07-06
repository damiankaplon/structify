package io.structify.infrastructure.row.persistence

import io.structify.domain.row.Cell
import io.structify.domain.row.Row
import io.structify.domain.row.RowRepository
import io.structify.infrastructure.db.NoEntityFoundException
import io.structify.infrastructure.db.OptimisticLockException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ExposedRowRepository : RowRepository {

	override suspend fun save(row: Row): Row {
        persistRoot(row)

        // Cells are wholly owned by the row: wipe and re-insert the collection.
        // The delete cascades to nested cells via the parent_cell_id FK, so no
        // orphan bookkeeping is needed here.
		CellsTable.deleteWhere { CellsTable.rowId eq row.id }
		row.cells.forEach { cell ->
			persistCellHierarchy(row.id, cell, parentCellId = null)
		}

		return row
	}

    /**
     * Optimistically inserts a new row or updates the existing one, guarding
     * against concurrent modifications via the [RowsTable.optLock] counter.
     */
    private fun persistRoot(row: Row) {
        val updated = RowsTable.update({ (RowsTable.id eq row.id) and (RowsTable.optLock eq row.optLock) }) { dbRow ->
            dbRow[RowsTable.versionId] = row.versionId
            dbRow[RowsTable.optLock] = row.optLock + 1
        }
        if (updated == 0) {
            val exists = !RowsTable.selectAll().where { RowsTable.id eq row.id }.empty()
            if (exists) throw OptimisticLockException("row", row.id)
            RowsTable.insert { dbRow ->
                dbRow[RowsTable.id] = row.id
                dbRow[RowsTable.versionId] = row.versionId
                dbRow[RowsTable.optLock] = 0
            }
        } else {
            row.optLock += 1
        }
    }

	override suspend fun findById(id: UUID): Row? {
		return RowsTable.selectAll()
			.where { RowsTable.id eq id }
			.singleOrNull()
			?.let { row ->
				return Row(
					id = row[RowsTable.id],
					versionId = row[RowsTable.versionId],
                    cells = fetchCells(row[RowsTable.id]),
                    optLock = row[RowsTable.optLock]
				)
			}
	}

	override suspend fun findByIdOrThrow(id: UUID): Row =
		findById(id) ?: throw NoEntityFoundException("Row with id $id not found")

	private fun fetchCells(rowId: UUID): Set<Cell> {
		// Fetch only top-level cells (those without a parent)
		val topLevelCellRows = CellsTable.selectAll()
			.where { (CellsTable.rowId eq rowId) and (CellsTable.parentCellId.isNull()) }

		return topLevelCellRows.mapTo(linkedSetOf()) { row ->
			fetchCellWithChildren(row[CellsTable.id])
		}
	}

	private fun fetchCellWithChildren(cellId: Long): Cell {
		val row = CellsTable.selectAll()
			.where { CellsTable.id eq cellId }
			.single()

		// Recursively fetch child cells
		val childRows = CellsTable.selectAll()
			.where { CellsTable.parentCellId eq cellId }

		val children = childRows.mapTo(linkedSetOf()) { childRow ->
			fetchCellWithChildren(childRow[CellsTable.id])
		}

		return Cell(
			columnDefinitionId = row[CellsTable.columnDefinitionId],
			value = row[CellsTable.value],
			children = children
		)
	}

	private fun persistCellHierarchy(rowId: UUID, cell: Cell, parentCellId: Long?): Long {
		// Insert cell and get the generated ID
		val cellId = CellsTable.insert { dbRow ->
			dbRow[CellsTable.rowId] = rowId
			dbRow[CellsTable.columnDefinitionId] = cell.columnDefinitionId
			dbRow[CellsTable.value] = cell.value
			dbRow[CellsTable.parentCellId] = parentCellId
		}[CellsTable.id]

		// Recursively persist children
		cell.children.forEach { childCell ->
			persistCellHierarchy(rowId, childCell, parentCellId = cellId)
		}

		return cellId
	}
}
