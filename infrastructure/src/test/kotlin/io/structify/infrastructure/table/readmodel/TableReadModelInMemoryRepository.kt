package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.table.readmodel.TableReadModelRepository.Table
import java.util.*

internal class TableReadModelInMemoryRepository : TableReadModelRepository {

	private val userTables = mutableMapOf<UUID, MutableSet<Table>>()

	override suspend fun findAllByUserId(userId: UUID): Set<Table> {
		return userTables[userId]?.toSet() ?: emptySet()
	}

	fun addTable(userId: UUID, table: Table) {
		userTables.getOrPut(userId) { linkedSetOf() }.add(table)
	}

	override suspend fun create(id: UUID, userId: UUID, name: String, description: String) {
		userTables.getOrPut(userId) { linkedSetOf() }
			.add(Table(id = id.toString(), name = name, description = description))
	}

	fun clear() {
		userTables.clear()
	}
}
