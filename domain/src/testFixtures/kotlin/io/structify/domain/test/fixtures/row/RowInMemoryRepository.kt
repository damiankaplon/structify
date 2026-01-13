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

	fun findAll(): List<Row> {
		return rows.values.toList()
	}

	override suspend fun findById(id: UUID): Row? {
		return rows.values.firstOrNull { it.id == id }
	}

	override suspend fun findByIdOrThrow(id: UUID): Row =
		findById(id) ?: error("Could not find row with id: $id")
}
