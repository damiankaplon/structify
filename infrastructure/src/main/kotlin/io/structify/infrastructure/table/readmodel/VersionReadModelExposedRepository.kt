package io.structify.infrastructure.table.readmodel

import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.infrastructure.db.NoEntityFoundException
import io.structify.infrastructure.kotlinx.serialization.toKotlinx
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnDefinition
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import io.structify.infrastructure.table.readmodel.persistence.ColumnReadModelTable
import io.structify.infrastructure.table.readmodel.persistence.TableReadModelTable
import io.structify.infrastructure.table.readmodel.persistence.VersionColumnReadModelTable
import io.structify.infrastructure.table.readmodel.persistence.VersionReadModelTable
import org.jetbrains.exposed.sql.*
import java.util.*
import io.structify.domain.table.model.Version as DomainVersion
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnType as ReadModelColumnType

class VersionReadModelExposedRepository : VersionReadModelRepository {

	override suspend fun findAllVersionsByTableId(userId: UUID, tableId: UUID): Set<Version> {
		return VersionReadModelTable.innerJoin(TableReadModelTable).selectAll()
			.where { (VersionReadModelTable.tableId eq tableId) and (TableReadModelTable.userId eq userId) }
			.toList()
			.mapTo(linkedSetOf()) { versionRow: ResultRow ->
				val columns = fetchColumns(versionRow[VersionReadModelTable.id])
				Version(
					id = versionRow[VersionReadModelTable.id].toString(),
					columns = columns,
					orderNumber = versionRow[VersionReadModelTable.orderNumber]
				)
			}
	}

	override suspend fun findCurrentVersionByTableId(userId: UUID, tableId: UUID): Version? {
		val versionRow = VersionReadModelTable.innerJoin(TableReadModelTable)
			.selectAll()
			.where { (VersionReadModelTable.tableId eq tableId) and (TableReadModelTable.userId eq userId) }
			.orderBy(VersionReadModelTable.orderNumber to SortOrder.DESC)
			.limit(1)
			.firstOrNull() ?: return null

		val columns = fetchColumns(versionRow[VersionReadModelTable.id])

		return Version(
			id = versionRow[VersionReadModelTable.id].toString(),
			columns = columns,
			orderNumber = versionRow[VersionReadModelTable.orderNumber]
		)
	}

	override suspend fun findCurrentVersionByTableIdOrThrow(userId: UUID, tableId: UUID): Version {
		return findCurrentVersionByTableId(userId, tableId)
			?: throw NoEntityFoundException("Could not find current version of table with id: $tableId, user id: $userId")
	}

	override suspend fun upsertVersion(tableId: UUID, userId: UUID, version: DomainVersion) {
		VersionReadModelTable.upsert(VersionReadModelTable.id) { row ->
			row[VersionReadModelTable.id] = version.id
			row[VersionReadModelTable.tableId] = tableId
			row[VersionReadModelTable.orderNumber] = version.orderNumber
		}

		// Persist top-level columns and their hierarchies, then associate them with the version
		version.columns.forEach { column ->
			persistColumnHierarchy(column, parentId = null)
			VersionColumnReadModelTable.upsert(
				VersionColumnReadModelTable.versionId,
				VersionColumnReadModelTable.columnDefinitionId
			) { row ->
				row[VersionColumnReadModelTable.versionId] = version.id
				row[VersionColumnReadModelTable.columnDefinitionId] = column.id
			}
		}
	}

	private fun persistColumnHierarchy(column: Column, parentId: UUID?) {
		ColumnReadModelTable.upsert(ColumnReadModelTable.id) { row ->
			row[ColumnReadModelTable.id] = column.id
			row[ColumnReadModelTable.name] = column.definition.name
			row[ColumnReadModelTable.description] = column.definition.description
			row[ColumnReadModelTable.typeName] = toTypeName(column.definition.type)
			row[ColumnReadModelTable.stringFormat] = toStringFormat(column.definition.type)
			row[ColumnReadModelTable.optional] = column.definition.optional
			row[ColumnReadModelTable.parentColumnId] = parentId
		}

		column.children.forEach { child ->
			persistColumnHierarchy(child, parentId = column.id)
		}
	}

	private fun toTypeName(type: ColumnType): String = when (type) {
		is ColumnType.StringType -> "STRING"
		is ColumnType.NumberType -> "NUMBER"
		is ColumnType.ObjectType -> "OBJECT"
	}

	private fun toStringFormat(type: ColumnType): String? = when (type) {
		is ColumnType.StringType -> type.format?.name
		is ColumnType.NumberType -> null
		is ColumnType.ObjectType -> null
	}

	private fun fetchColumns(versionId: UUID): List<ColumnDefinition> {
		// Fetch only top-level columns for this version
		val topLevelColumnIds = ColumnReadModelTable.join(
			VersionColumnReadModelTable,
			JoinType.INNER,
			VersionColumnReadModelTable.columnDefinitionId,
			ColumnReadModelTable.id
		)
			.selectAll()
			.where { VersionColumnReadModelTable.versionId eq versionId }
			.map { cRow -> cRow[ColumnReadModelTable.id] }

		return topLevelColumnIds.map { columnId ->
			fetchColumnWithChildren(columnId)
		}
	}

	private fun fetchColumnWithChildren(columnId: UUID): ColumnDefinition {
		val row = ColumnReadModelTable.selectAll()
			.where { ColumnReadModelTable.id eq columnId }
			.single()

		// Recursively fetch child columns
		val childIds = ColumnReadModelTable.selectAll()
			.where { ColumnReadModelTable.parentColumnId eq columnId }
			.map { it[ColumnReadModelTable.id] }

		val children = childIds.map { childId ->
			fetchColumnWithChildren(childId)
		}

		return ColumnDefinition(
			id = row[ColumnReadModelTable.id].toKotlinx(),
			name = row[ColumnReadModelTable.name],
			description = row[ColumnReadModelTable.description],
			type = ReadModelColumnType(
				type = row[ColumnReadModelTable.typeName],
				format = row[ColumnReadModelTable.stringFormat]
			),
			optional = row[ColumnReadModelTable.optional],
			children = children
		)
	}
}
