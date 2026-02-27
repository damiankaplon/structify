package io.structify.infrastructure.row.persistence

import io.structify.domain.row.Cell
import io.structify.domain.row.Row
import io.structify.domain.row.RowRepository
import io.structify.infrastructure.db.NoEntityFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.upsert
import java.util.UUID

class ExposedRowRepository : RowRepository {

	override suspend fun save(row: Row): Row {
		RowsTable.upsert(RowsTable.id) { dbRow ->
			dbRow.updateWith(row)
		}

		// Persist cells hierarchically
		row.cells.forEach { cell ->
			persistCellHierarchy(row.id, cell, parentCellId = null)
		}
		
		row.removeStaleCells()
		return row
	}

	override suspend fun findById(id: UUID): Row? {
		return RowsTable.selectAll()
			.where { RowsTable.id eq id }
			.singleOrNull()
			?.let { row ->
				return Row(
					id = row[RowsTable.id],
					versionId = row[RowsTable.versionId],
					cells = fetchCells(row[RowsTable.id])
				)
			}
	}

	override suspend fun findByIdOrThrow(id: UUID): Row =
		findById(id) ?: throw NoEntityFoundException("Row with id $id not found")

	private fun UpdateBuilder<*>.updateWith(row: Row) {
		this[RowsTable.id] = row.id
		this[RowsTable.versionId] = row.versionId
	}

	private fun Row.removeStaleCells() {
		val rowId = this.id
		val allCellDefinitionIds = flattenCells(this.cells).map { it.columnDefinitionId }
		CellsTable.deleteWhere {
			(CellsTable.rowId eq rowId) and (CellsTable.columnDefinitionId notInList allCellDefinitionIds)
		}
	}

	private fun flattenCells(cells: Set<Cell>): List<Cell> {
		return cells.flatMap { cell ->
			listOf(cell) + flattenCells(cell.children)
		}
	}

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
