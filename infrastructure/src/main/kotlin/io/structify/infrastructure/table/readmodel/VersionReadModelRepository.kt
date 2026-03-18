package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.kotlinx.serialization.Uuid
import kotlinx.serialization.Serializable
import java.util.*
import io.structify.domain.table.model.Version as DomainVersion

interface VersionReadModelRepository {

	suspend fun findAllVersionsByTableId(userId: UUID, tableId: UUID): Set<Version>
	suspend fun findCurrentVersionByTableId(userId: UUID, tableId: UUID): Version?
	suspend fun findCurrentVersionByTableIdOrThrow(userId: UUID, tableId: UUID): Version
	suspend fun upsertVersion(tableId: UUID, userId: UUID, version: DomainVersion)

	@Serializable
	data class Version(
		val id: String,
		val columns: List<ColumnDefinition>,
		val orderNumber: Int,
	)

	@Serializable
	data class ColumnDefinition(
		val id: Uuid,
		val name: String,
		val description: String,
		val type: ColumnType,
		val optional: Boolean,
		val children: List<ColumnDefinition> = emptyList(),
	)

	@Serializable
	data class ColumnType(
		val type: String,
		val format: String? = null,
	)
}
