package io.structify.domain.table

import io.structify.domain.table.model.Table
import java.util.UUID

interface TableRepository {

    suspend fun persist(table: Table): Table

    suspend fun findById(userId: UUID, tableId: UUID): Table?

	suspend fun findByIdThrow(userId: UUID, tableId: UUID): Table

	suspend fun findByVersionIdOrThrow(versionId: UUID): Table
}
