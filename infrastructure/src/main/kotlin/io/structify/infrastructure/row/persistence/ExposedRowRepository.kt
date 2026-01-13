package io.structify.infrastructure.row.persistence

import io.structify.domain.row.Cell
import io.structify.domain.row.Row
import io.structify.domain.row.RowRepository
import io.structify.infrastructure.db.NoEntityFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.upsert
import java.util.UUID

class ExposedRowRepository : RowRepository {

	override suspend fun save(row: Row): Row {
		RowsTable.upsert(RowsTable.id) { dbRow ->
			dbRow.updateWith(row)
		}

		row.cells.forEach { cell ->
			CellsTable.upsert(CellsTable.rowId, CellsTable.columnDefinitionId) { dbRow ->
				dbRow.updateWith(row.id, cell)
			}
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

	private fun UpdateBuilder<*>.updateWith(rowId: UUID, cell: Cell) {
		this[CellsTable.rowId] = rowId
		this[CellsTable.columnDefinitionId] = cell.columnDefinitionId
		this[CellsTable.value] = cell.value
	}

	private fun Row.removeStaleCells() {
		val rowId = this.id
		val definitionIds = this.cells.map(Cell::columnDefinitionId)
		CellsTable.deleteWhere {
			(CellsTable.rowId eq rowId) and (CellsTable.columnDefinitionId notInList definitionIds)
		}
	}

	private fun fetchCells(rowId: UUID): Set<Cell> {
		return CellsTable.selectAll()
			.where { CellsTable.rowId eq rowId }
			.mapTo(linkedSetOf()) { row ->
				Cell(
					columnDefinitionId = row[CellsTable.columnDefinitionId],
					value = row[CellsTable.value],
				)
			}
	}
}
