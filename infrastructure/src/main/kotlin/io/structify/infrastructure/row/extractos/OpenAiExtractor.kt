package io.structify.infrastructure.row.extractos

import io.structify.domain.row.Cell
import io.structify.domain.row.RowExtractor
import io.structify.domain.table.model.Column

class OpenAiExtractor : RowExtractor {

	override suspend fun extract(columns: List<Column>, content: String): Set<Cell> {
		return emptySet()
	}
}
