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
import io.structify.domain.table.model.ColumnType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

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
					model = "gpt-5-mini",
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
							description = "Extracts data from the PDF file content"
						)
					)
				)
			)
		}
		val openAiResult = response.body<ChatGptResponsesApiResponse>().output.first { it.content != null }.content!!.first().text
			.let { json.parseToJsonElement(it) }
			.jsonObject
		
		val cells = columns.map { column ->
			extractCell(column, openAiResult)
		}
		return cells.toSet()
	}

	private fun extractCell(column: Column, jsonData: JsonObject): Cell {
		val columnId = column.id
		val jsonValue = jsonData.getValue(column.definition.name)

		return when (column.definition.type) {
			is ColumnType.StringType, is ColumnType.NumberType -> {
				// Leaf column - extract primitive value
				val cellValue = if (jsonValue is JsonPrimitive) {
					jsonValue.content
				} else {
					jsonValue.toString()
				}
				Cell(columnId, cellValue)
			}
			is ColumnType.ObjectType -> {
				// Object column - recursively extract children
				val nestedObject = jsonValue as JsonObject
				val childCells = column.children.map { childColumn ->
					extractCell(childColumn, nestedObject)
				}.toSet()
				Cell(columnId, "", childCells)
			}
		}
	}
}
