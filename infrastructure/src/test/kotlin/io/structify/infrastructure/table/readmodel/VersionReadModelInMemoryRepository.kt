package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.db.NoEntityFoundException
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import java.util.UUID

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

	fun addVersion(userId: UUID, tableId: UUID, version: Version) {
		versions.getOrPut(userId to tableId) { mutableListOf() }.add(version)
	}

	fun clear() {
		versions.clear()
	}
}
