package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.table.readmodel.TableReadModelRepository.Table
import java.util.UUID

internal class TableReadModelInMemoryRepository : TableReadModelRepository {

	private val userTables = mutableMapOf<UUID, MutableSet<Table>>()

	override suspend fun findAllByUserId(userId: UUID): Set<Table> {
		return userTables[userId]?.toSet() ?: emptySet()
	}

	fun addTable(userId: UUID, table: Table) {
		userTables.getOrPut(userId) { linkedSetOf() }.add(table)
	}

	override suspend fun addDescription(id: UUID, description: String) {
	}

	fun clear() {
		userTables.clear()
	}
}
