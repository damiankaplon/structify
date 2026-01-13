package io.structify.domain.table.model

import java.util.UUID

data class ColumnDefinition(
	val id: UUID = UUID.randomUUID(),
	val name: String,
	val description: String,
	val type: ColumnType,
	val optional: Boolean,
) {

	fun sameDefinitionAs(other: ColumnDefinition): Boolean =
		this.name == other.name &&
			this.description == other.description &&
			this.type == other.type &&
			this.optional == other.optional
}
