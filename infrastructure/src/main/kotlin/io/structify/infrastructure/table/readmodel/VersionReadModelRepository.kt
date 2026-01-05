package io.structify.infrastructure.table.readmodel

import kotlinx.serialization.Serializable
import java.util.UUID

interface VersionReadModelRepository {

	suspend fun findAllVersionsByTableId(userId: UUID, tableId: UUID): Set<Version>
	suspend fun findCurrentVersionByTableId(userId: UUID, tableId: UUID): Version?
	suspend fun findCurrentVersionByTableIdOrThrow(userId: UUID, tableId: UUID): Version

	@Serializable
	data class Version(
		val id: String,
		val columns: List<ColumnDefinition>,
		val orderNumber: Int,
	)

	@Serializable
	data class ColumnDefinition(
		val id: Long,
		val name: String,
		val description: String,
		val type: ColumnType,
		val optional: Boolean,
	)

	@Serializable
	data class ColumnType(
		val type: String,
		val format: String? = null,
	)
}
