package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.db.NoEntityFoundException
import io.structify.infrastructure.table.persistence.TableColumnsTable
import io.structify.infrastructure.table.persistence.TableVersionsTable
import io.structify.infrastructure.table.persistence.TablesTable
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnDefinition
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnType
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class VersionReadModelExposedRepository : VersionReadModelRepository {

	override suspend fun findAllVersionsByTableId(userId: UUID, tableId: UUID): Set<Version> {
		return TableVersionsTable.innerJoin(TablesTable).selectAll()
			.where { (TableVersionsTable.tableId eq tableId) and (TablesTable.userId eq userId) }
			.toList()
			.mapTo(linkedSetOf()) { versionRow: ResultRow ->
				val columns = fetchColumns(versionRow[TableVersionsTable.id])
				Version(
					id = versionRow[TableVersionsTable.id].toString(),
					columns = columns,
					orderNumber = versionRow[TableVersionsTable.orderNumber]
				)
			}
	}

	override suspend fun findCurrentVersionByTableId(userId: UUID, tableId: UUID): Version? {
		val versionRow = TableVersionsTable.innerJoin(TablesTable)
			.selectAll()
			.where { (TableVersionsTable.tableId eq tableId) and (TablesTable.userId eq userId) }
			.orderBy(TableVersionsTable.orderNumber to SortOrder.DESC)
			.limit(1)
			.firstOrNull() ?: return null

		val columns = fetchColumns(versionRow[TableVersionsTable.id])

		return Version(
			id = versionRow[TableVersionsTable.id].toString(),
			columns = columns,
			orderNumber = versionRow[TableVersionsTable.orderNumber]
		)
	}

	override suspend fun findCurrentVersionByTableIdOrThrow(userId: UUID, tableId: UUID): Version {
		return findCurrentVersionByTableId(userId, tableId)
			?: throw NoEntityFoundException("Could not find current version of table with id: $tableId, user id: $userId")
	}

	private fun fetchColumns(versionId: UUID): List<ColumnDefinition> {
		return TableColumnsTable.selectAll()
			.where { TableColumnsTable.versionId eq versionId }
			.map { cRow ->
				ColumnDefinition(
					id = cRow[TableColumnsTable.id],
					name = cRow[TableColumnsTable.name],
					description = cRow[TableColumnsTable.description],
					type = ColumnType(
						type = cRow[TableColumnsTable.typeName],
						format = cRow[TableColumnsTable.stringFormat]
					),
					optional = cRow[TableColumnsTable.optional]
				)
			}
	}
}
