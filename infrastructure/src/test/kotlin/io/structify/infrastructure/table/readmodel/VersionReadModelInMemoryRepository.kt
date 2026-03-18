package io.structify.infrastructure.table.readmodel

import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.infrastructure.db.NoEntityFoundException
import io.structify.infrastructure.kotlinx.serialization.toKotlinx
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnDefinition
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import java.util.*
import io.structify.domain.table.model.Version as DomainVersion
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnType as ReadModelColumnType

internal class VersionReadModelInMemoryRepository : VersionReadModelRepository {

	private val versions = mutableMapOf<Pair<UUID, UUID>, MutableList<Version>>()

	override suspend fun findAllVersionsByTableId(userId: UUID, tableId: UUID): Set<Version> {
		return versions[userId to tableId]?.toSet() ?: emptySet()
	}

	override suspend fun findCurrentVersionByTableId(userId: UUID, tableId: UUID): Version? {
		return versions[userId to tableId]?.maxByOrNull { it.orderNumber }
	}

	override suspend fun findCurrentVersionByTableIdOrThrow(userId: UUID, tableId: UUID): Version {
		return findCurrentVersionByTableId(userId, tableId)
			?: throw NoEntityFoundException("Version not found for table $tableId")
	}

	override suspend fun upsertVersion(tableId: UUID, userId: UUID, version: DomainVersion) {
		val readModelVersion = Version(
			id = version.id.toString(),
			columns = version.columns.map { it.toReadModel() },
			orderNumber = version.orderNumber,
		)
		val list = versions.getOrPut(userId to tableId) { mutableListOf() }
		val existing = list.indexOfFirst { it.id == readModelVersion.id }
		if (existing >= 0) {
			list[existing] = readModelVersion
		} else {
			list.add(readModelVersion)
		}
	}

	fun addVersion(userId: UUID, tableId: UUID, version: Version) {
		versions.getOrPut(userId to tableId) { mutableListOf() }.add(version)
	}

	private fun Column.toReadModel(): ColumnDefinition = ColumnDefinition(
		id = id.toKotlinx(),
		name = definition.name,
		description = definition.description,
		type = definition.type.toReadModel(),
		optional = definition.optional,
		children = children.map { it.toReadModel() },
	)

	private fun ColumnType.toReadModel(): ReadModelColumnType = when (this) {
		is ColumnType.StringType -> ReadModelColumnType(type = "STRING", format = format?.name)
		is ColumnType.NumberType -> ReadModelColumnType(type = "NUMBER")
		is ColumnType.ObjectType -> ReadModelColumnType(type = "OBJECT")
	}
}
