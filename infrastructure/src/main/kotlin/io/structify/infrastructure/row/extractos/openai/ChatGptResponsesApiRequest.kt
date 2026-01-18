package io.structify.infrastructure.row.extractos.openai

import kotlinx.serialization.Serializable

@Serializable
data class ChatGptResponsesApiRequest(
	val model: String? = null,
	val input: Set<ContentInput>,
	val text: Text,
) {

	@Serializable
	data class ContentInput(
		val role: String,
		val content: Set<Content>,
	) {

		@Serializable
		data class Content(
			val type: String,
			val text: String? = null,
		)
	}

	@Serializable
	data class Text(
		val format: Format,
	) {

		@Serializable
		data class Format(
			val name: String,
			val description: String,
			val type: String,
			val schema: Schema,
			val strict: Boolean = true,
		) {

			@Serializable
			data class Schema(
				val name: String,
				val type: String,
				val properties: Map<String, Property>,
				val additionalProperties: Boolean = false,
				val required: Set<String>,
			) {

				@Serializable
				data class Property(
					val type: Set<String>,
					val description: String,
				)
			}
		}
	}
}
