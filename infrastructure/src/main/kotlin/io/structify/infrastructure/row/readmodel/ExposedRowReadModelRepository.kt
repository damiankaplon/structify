package io.structify.infrastructure.row.readmodel

import io.structify.infrastructure.row.persistence.CellsTable
import io.structify.infrastructure.row.persistence.RowsTable
import io.structify.infrastructure.row.readmodel.RowReadModelRepository.Cell
import io.structify.infrastructure.row.readmodel.RowReadModelRepository.Row
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class ExposedRowReadModelRepository : RowReadModelRepository {

	override suspend fun findAllByTableId(tableId: UUID): Set<Row> {
		val rowIds = RowsTable.selectAll()
			.where { RowsTable.tableId eq tableId }
			.map { it[RowsTable.id] }

		return rowIds.mapTo(linkedSetOf()) { rowId ->
			val cells = CellsTable.selectAll()
				.where { CellsTable.rowId eq rowId }
				.mapTo(linkedSetOf()) { row ->
					Cell(
						columnId = row[CellsTable.columnId],
						value = row[CellsTable.value],
					)
				}
			Row(
				id = rowId.toString(),
				cells = cells,
			)
		}
	}
}
