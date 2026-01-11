package io.structify.infrastructure.row.extractos

import io.structify.domain.row.Row
import io.structify.domain.row.RowExtractor
import io.structify.domain.table.model.ColumnDefinition

class OpenAiExtractor : RowExtractor {

	override suspend fun extract(columns: List<ColumnDefinition>, fromContent: String): Row {
		TODO("Not yet implemented")
	}
}
