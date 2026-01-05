package io.structify.infrastructure.row.readmodel

import kotlinx.serialization.Serializable
import java.util.UUID

interface RowReadModelRepository {

	suspend fun findAllByTableId(tableId: UUID): Set<Row>

	@Serializable
	data class Row(
		val id: String,
		val cells: Set<Cell>,
	)

	@Serializable
	data class Cell(
		val columnId: Int,
		val value: String,
	)
}
