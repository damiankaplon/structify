package io.structify.domain.test.fixtures.row

import io.structify.domain.row.Row
import io.structify.domain.row.RowRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RowInMemoryRepository : RowRepository {

	private val rows = ConcurrentHashMap<UUID, Row>()

	override suspend fun save(row: Row): Row {
		rows[row.id] = row
		return row
	}

	override suspend fun findByTableId(tableId: UUID): Set<Row> {
		return rows.values.filter { it.tableId == tableId }.toSet()
	}

	fun findAll(): List<Row> {
		return rows.values.toList()
	}

	fun findById(id: UUID): Row? {
		return rows[id]
	}
}
