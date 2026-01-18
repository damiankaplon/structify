package io.structify.infrastructure.row.extractos.openai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import io.structify.domain.row.Cell
import io.structify.domain.row.RowExtractor
import io.structify.domain.table.model.Column
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenAiExtractor(
	private val chatGptHttpClient: HttpClient,
	private val json: Json,
) : RowExtractor {

	override suspend fun extract(
		columns: List<Column>,
		content: String,
	): Set<Cell> {
		val response = chatGptHttpClient.post {
			url { path("v1/responses") }
			contentType(ContentType.Application.Json)
			setBody(
				ChatGptResponsesApiRequest(
					input = setOf(
						ChatGptResponsesApiRequest.ContentInput(
							role = "user",
							content = setOf(
								ChatGptResponsesApiRequest.ContentInput.Content(
									type = "input_text",
									text = content
								)
							)
						)
					),
					text = ChatGptResponsesApiRequest.Text(
						format = ChatGptResponsesApiRequest.Text.Format(
							name = "text",
							type = "json_schema",
							schema = generateSchema(columns),
							description = "Extracts the road report data from the PDF file"
						)
					)
				)
			)
		}
		val openAiResult = response.body<ChatGptResponsesApiResponse>().output.first().content.first().text
			.let { json.parseToJsonElement(it) }
		val cells = columns.map { column ->
			val columnId = column.id
			val cellValue = openAiResult.jsonObject.getValue(column.definition.name).jsonPrimitive.content
			Cell(columnId, cellValue)
		}
		return cells.toSet()
	}
}
