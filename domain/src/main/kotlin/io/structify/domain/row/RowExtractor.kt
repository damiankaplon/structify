package io.structify.domain.row

import io.structify.domain.table.model.Column

fun interface RowExtractor {

	suspend fun extract(columns: List<Column>, content: String): Set<Cell>
}
