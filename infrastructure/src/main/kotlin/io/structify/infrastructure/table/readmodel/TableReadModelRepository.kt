package io.structify.infrastructure.table.readmodel

import kotlinx.serialization.Serializable
import java.util.UUID

interface TableReadModelRepository {

	suspend fun findAllByUserId(userId: UUID): Set<Table>

	@Serializable
	data class Table(
		val id: String,
		val name: String,
	)
}
