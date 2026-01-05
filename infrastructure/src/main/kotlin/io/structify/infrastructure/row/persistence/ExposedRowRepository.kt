package io.structify.infrastructure.row.persistence

import io.structify.domain.row.Cell
import io.structify.domain.row.Row
import io.structify.domain.row.RowRepository
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
			CellsTable.upsert(CellsTable.rowId, CellsTable.columnId) { dbRow ->
				dbRow.updateWith(row.id, cell)
			}
		}
		row.removeStaleCells()
		return row
	}

	override suspend fun findByTableId(tableId: UUID): Set<Row> {
		val rowIds = RowsTable.selectAll()
			.where { RowsTable.tableId eq tableId }
			.map { it[RowsTable.id] }

		return rowIds.mapTo(linkedSetOf()) { rowId ->
			val cells = fetchCells(rowId)
			Row(
				id = rowId,
				tableId = tableId,
				cells = cells,
			)
		}
	}

	private fun UpdateBuilder<*>.updateWith(row: Row) {
		this[RowsTable.id] = row.id
		this[RowsTable.tableId] = row.tableId
	}

	private fun UpdateBuilder<*>.updateWith(rowId: UUID, cell: Cell) {
		this[CellsTable.rowId] = rowId
		this[CellsTable.columnId] = cell.columnId
		this[CellsTable.value] = cell.value
	}

	private fun Row.removeStaleCells() {
		val rowId = this.id
		val columnIds = this.cells.map(Cell::columnId)
		CellsTable.deleteWhere {
			(CellsTable.rowId eq rowId) and (CellsTable.columnId notInList columnIds)
		}
	}

	private fun fetchCells(rowId: UUID): Set<Cell> {
		return CellsTable.selectAll()
			.where { CellsTable.rowId eq rowId }
			.mapTo(linkedSetOf()) { row ->
				Cell(
					columnId = row[CellsTable.columnId],
					value = row[CellsTable.value],
				)
			}
	}
}
