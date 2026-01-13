package io.structify.infrastructure.row.readmodel

import io.structify.infrastructure.row.persistence.CellsTable
import io.structify.infrastructure.row.persistence.RowsTable
import io.structify.infrastructure.row.readmodel.RowReadModelRepository.Cell
import io.structify.infrastructure.row.readmodel.RowReadModelRepository.Row
import io.structify.infrastructure.table.persistence.TableVersionsTable
import io.structify.infrastructure.table.persistence.TablesTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class ExposedRowReadModelRepository : RowReadModelRepository {

	override suspend fun findAllByTableId(tableId: UUID): Set<Row> {
		val rowIds = RowsTable.join(TableVersionsTable, joinType = JoinType.INNER, onColumn = RowsTable.versionId, otherColumn = TableVersionsTable.id)
			.join(TablesTable, joinType = JoinType.INNER, onColumn = TablesTable.id, otherColumn = TableVersionsTable.tableId)
			.selectAll()
			.where { TablesTable.id eq tableId }
			.map { it[RowsTable.id] }

		return rowIds.mapTo(linkedSetOf()) { rowId ->
			val cells = CellsTable.selectAll()
				.where { CellsTable.rowId eq rowId }
				.mapTo(linkedSetOf()) { row ->
					Cell(
						columnDefinitionId = row[CellsTable.columnDefinitionId].toString(),
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
