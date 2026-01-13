package io.structify.domain.row

import java.util.UUID

interface RowRepository {

	suspend fun save(row: Row): Row
	suspend fun findById(id: UUID): Row?
	suspend fun findByIdOrThrow(id: UUID): Row
}
