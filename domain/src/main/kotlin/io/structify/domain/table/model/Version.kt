package io.structify.domain.table.model

import java.util.UUID

data class Version(
	val id: UUID = UUID.randomUUID(),
    val columns: List<ColumnDefinition>,
	val orderNumber: Int,
)
