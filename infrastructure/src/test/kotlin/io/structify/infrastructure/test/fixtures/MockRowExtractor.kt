package io.structify.infrastructure.test.fixtures

import io.structify.domain.row.Row
import io.structify.domain.row.RowExtractor
import io.structify.domain.table.model.ColumnDefinition
import java.util.UUID

class MockRowExtractor : RowExtractor {

	override suspend fun extract(columns: List<ColumnDefinition>, content: String): Row {
		return Row(
			tableId = UUID.randomUUID(),
			cells = linkedSetOf()
		)
	}
}
