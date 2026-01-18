package io.structify.infrastructure.row.extractos.openai

import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType

fun generateSchema(columns: List<Column>): ChatGptResponsesApiRequest.Text.Format.Schema {
	val properties = columns.associate { it.definition.toProperty() }
	return ChatGptResponsesApiRequest.Text.Format.Schema(
		name = "",
		type = "object",
		properties = properties,
		required = columns.map { it.definition.name }.toSet()
	)
}

private fun Column.Definition.toProperty(): Pair<String, ChatGptResponsesApiRequest.Text.Format.Schema.Property> = Pair(
	name,
	ChatGptResponsesApiRequest.Text.Format.Schema.Property(
		type = buildSet {
			when (this@toProperty.type) {
				is ColumnType.StringType -> add("string")
				is ColumnType.NumberType -> add("number")
			}
			if (this@toProperty.optional) {
				add("null")
			}
		},
		description = description
	)
)
