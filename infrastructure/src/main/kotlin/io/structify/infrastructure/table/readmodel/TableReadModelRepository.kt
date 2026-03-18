package io.structify.infrastructure.table.readmodel

import kotlinx.serialization.Serializable
import java.util.*

interface TableReadModelRepository {

	suspend fun findAllByUserId(userId: UUID): Set<Table>
	suspend fun create(id: UUID, userId: UUID, name: String, description: String)

	@Serializable
	data class Table(
		val id: String,
		val name: String,
		val description: String,
	)
}
