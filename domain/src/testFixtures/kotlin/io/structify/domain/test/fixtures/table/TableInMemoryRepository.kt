package io.structify.domain.test.fixtures.table

import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.Table
import java.util.UUID

class TableInMemoryRepository : TableRepository {

	private val tables = mutableMapOf<UUID, Table>()

	override suspend fun persist(table: Table): Table {
		tables[table.id] = table
		return table
	}

	override suspend fun findById(userId: UUID, tableId: UUID): Table? {
		return tables[tableId]?.takeIf { it.userId == userId }
	}

	override suspend fun findByIdThrow(userId: UUID, tableId: UUID): Table {
		return findById(userId, tableId) ?: throw NoSuchElementException("Could not find table with id: $tableId, user id: $userId")
	}

	fun findAll(): List<Table> = tables.values.toList()
}
