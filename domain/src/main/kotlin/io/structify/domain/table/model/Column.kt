package io.structify.domain.table.model

import java.util.UUID

data class Column(
	val id: UUID = UUID.randomUUID(),
	val definition: Definition,
	val children: List<Column> = emptyList(),
) {

	data class Definition(
		val name: String,
		val description: String,
		val type: ColumnType,
		val optional: Boolean,
		val children: List<Definition> = emptyList(),
	)
}
