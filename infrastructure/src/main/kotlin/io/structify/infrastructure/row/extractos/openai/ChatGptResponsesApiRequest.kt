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
			val schema: Schema,
			val description: String,
			val type: String,
		) {

			@Serializable
			data class Schema(
				val name: String,
				val type: String,
				val properties: List<Map<String, Property>>,
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
