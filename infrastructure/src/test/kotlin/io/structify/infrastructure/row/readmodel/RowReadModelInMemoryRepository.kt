package io.structify.infrastructure.row.readmodel

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RowReadModelInMemoryRepository : RowReadModelRepository {

	private val rowsByTableId = ConcurrentHashMap<UUID, MutableSet<RowReadModelRepository.Row>>()

	override suspend fun findAllByTableId(tableId: UUID): Set<RowReadModelRepository.Row> {
		return rowsByTableId.getOrDefault(tableId, emptySet())
	}

	fun addRow(tableId: UUID, row: RowReadModelRepository.Row) {
		rowsByTableId.computeIfAbsent(tableId) { mutableSetOf() }.add(row)
	}
}
