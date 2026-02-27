package io.structify.infrastructure.row.extractos.openai

import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType

fun generateSchema(columns: List<Column>): ChatGptResponsesApiRequest.Text.Format.Schema {
	val properties = columns.associate { it.definition.toProperty() }
	val required = columns.filter { !it.definition.optional }.map { it.definition.name }.toSet()
	return ChatGptResponsesApiRequest.Text.Format.Schema(
		name = "",
		type = "object",
		properties = properties,
		required = required
	)
}

private fun Column.Definition.toProperty(): Pair<String, ChatGptResponsesApiRequest.Text.Format.Schema.Property> {
	val property = when (this.type) {
		is ColumnType.StringType -> {
			ChatGptResponsesApiRequest.Text.Format.Schema.Property(
				type = buildSet {
					add("string")
					if (this@toProperty.optional) {
						add("null")
					}
				},
				description = description
			)
		}
		is ColumnType.NumberType -> {
			ChatGptResponsesApiRequest.Text.Format.Schema.Property(
				type = buildSet {
					add("number")
					if (this@toProperty.optional) {
						add("null")
					}
				},
				description = description
			)
		}
		is ColumnType.ObjectType -> {
			// Recursively generate schema for nested objects
			val childProperties = children.associate { it.toProperty() }
			val requiredChildren = children.filter { !it.optional }.map { it.name }.toSet()
			ChatGptResponsesApiRequest.Text.Format.Schema.Property(
				type = buildSet {
					add("object")
					if (this@toProperty.optional) {
						add("null")
					}
				},
				description = description,
				properties = childProperties,
				required = requiredChildren,
				additionalProperties = false
			)
		}
	}
	return Pair(name, property)
}
