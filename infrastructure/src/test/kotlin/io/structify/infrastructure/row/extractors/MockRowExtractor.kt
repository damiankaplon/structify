package io.structify.infrastructure.row.extractors

import io.structify.domain.row.Cell
import io.structify.domain.row.RowExtractor
import io.structify.domain.table.model.Column

internal class MockRowExtractor : RowExtractor {

	var toReturn: Set<Cell> = emptySet()

    override suspend fun extract(columns: List<Column>, content: String): Set<Cell> {
		return toReturn
	}
}
