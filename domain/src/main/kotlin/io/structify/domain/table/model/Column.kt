package io.structify.domain.table.model

import java.util.UUID

data class Column(
	val id: UUID = UUID.randomUUID(),
	val definition: Definition,
) {

	data class Definition(
		val name: String,
		val description: String,
		val type: ColumnType,
		val optional: Boolean,
	)
}
