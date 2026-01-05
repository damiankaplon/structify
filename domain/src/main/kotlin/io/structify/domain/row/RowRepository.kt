package io.structify.domain.row

import java.util.UUID

interface RowRepository {

	suspend fun save(row: Row): Row
	suspend fun findByTableId(tableId: UUID): Set<Row>
}
