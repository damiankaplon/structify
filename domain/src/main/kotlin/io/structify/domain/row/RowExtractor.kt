package io.structify.domain.row

import io.structify.domain.table.model.ColumnDefinition

fun interface RowExtractor {

	suspend fun extract(columns: List<ColumnDefinition>, fromContent: String): Row
}
