package io.structify.infrastructure.table.persistence

import io.structify.domain.db.reflection.setPrivateProperty
import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Table
import io.structify.domain.table.model.Version
import io.structify.infrastructure.db.NoEntityFoundException
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.upsert
import java.util.UUID

class ExposedTableRepository : TableRepository {

	override suspend fun persist(table: Table): Table {
		TablesTable.upsert(TablesTable.id) { row ->
			row.updateWith(table)
		}

		table.versions.forEach { version ->
			TableVersionsTable.upsert(TableVersionsTable.id) { row ->
				row.updateWith(table.id, version)
			}

			// Persist columns hierarchically with parent references
			version.columns.forEach { column ->
				persistColumnHierarchy(column, parentId = null)
			}

			// Only store top-level columns in version_column_assoc
			version.columns.forEach { column ->
				VersionColumnTable.upsert(VersionColumnTable.versionId, VersionColumnTable.columnDefinitionId) { row ->
					row[VersionColumnTable.versionId] = version.id
					row[VersionColumnTable.columnDefinitionId] = column.id
				}
			}
			VersionColumnTable.deleteWhere {
				val versionColumnIds = version.columns.map(Column::id)
				(VersionColumnTable.versionId eq version.id) and (VersionColumnTable.columnDefinitionId notInList versionColumnIds)
			}
		}
		return table
	}

	override suspend fun findById(userId: UUID, tableId: UUID): Table? {
		val table = TablesTable.selectAll()
			.where { (TablesTable.id eq tableId) and (TablesTable.userId eq userId) }
			.firstOrNull()
			?.let { row ->
				Table(
					id = row[TablesTable.id],
					userId = row[TablesTable.userId],
					name = row[TablesTable.name],
				)
			} ?: return null

		val versions = TableVersionsTable.selectAll()
			.where { TableVersionsTable.tableId eq tableId }
			.toList()
			.mapTo(linkedSetOf()) { row ->
				val columns = fetchColumns(row[TableVersionsTable.id])
				Version(
					id = row[TableVersionsTable.id],
					orderNumber = row[TableVersionsTable.orderNumber],
					columns = columns,
				)
			}
		setPrivateProperty(table, table::versions.name, versions)

		return table
	}

	override suspend fun findByIdThrow(userId: UUID, tableId: UUID): Table {
		return findById(userId, tableId)
			?: throw NoEntityFoundException("Could not find table of user id: $userId and table id: $tableId")
	}

	override suspend fun findByVersionIdOrThrow(versionId: UUID): Table {
		val table = TablesTable.join(TableVersionsTable, JoinType.LEFT, TableVersionsTable.tableId, TablesTable.id)
			.select(TablesTable.columns)
			.where { (TableVersionsTable.id eq versionId) }
			.mapTo(linkedSetOf()) { row ->
				Table(
					id = row[TablesTable.id],
					userId = row[TablesTable.userId],
					name = row[TablesTable.name],
				)
			}.singleOrNull() ?: throw NoEntityFoundException("Could not find table of version id: $versionId")

		val versions = TableVersionsTable.selectAll()
			.where { TableVersionsTable.tableId eq table.id }
			.toList()
			.mapTo(linkedSetOf()) { row ->
				val columns = fetchColumns(row[TableVersionsTable.id])
				Version(
					id = row[TableVersionsTable.id],
					orderNumber = row[TableVersionsTable.orderNumber],
					columns = columns,
				)
			}
		setPrivateProperty(table, table::versions.name, versions)

		return table
	}

	private fun fromDbType(typeName: String, format: String?): ColumnType = when (typeName) {
		"STRING" -> ColumnType.StringType(format = format?.let { StringFormat.valueOf(it) })
		"NUMBER" -> ColumnType.NumberType
		"OBJECT" -> ColumnType.ObjectType
		else -> error("Unknown column type: $typeName")
	}

	private fun UpdateBuilder<*>.updateWith(table: Table) {
		this[TablesTable.id] = table.id
		this[TablesTable.userId] = table.userId
		this[TablesTable.name] = table.name
	}

	private fun UpdateBuilder<*>.updateWith(tableId: UUID, version: Version) {
		this[TableVersionsTable.id] = version.id
		this[TableVersionsTable.tableId] = tableId
		this[TableVersionsTable.orderNumber] = version.orderNumber
	}

	private fun UpdateBuilder<*>.updateWith(column: Column, parentId: UUID?) {
		this[TableColumnsTable.id] = column.id
		this[TableColumnsTable.name] = column.definition.name
		this[TableColumnsTable.description] = column.definition.description
		this[TableColumnsTable.typeName] = toTypeName(column.definition.type)
		this[TableColumnsTable.stringFormat] = toStringFormat(column.definition.type)
		this[TableColumnsTable.optional] = column.definition.optional
		this[TableColumnsTable.parentColumnId] = parentId
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

	private fun fetchColumns(versionId: UUID): List<Column> {
		// Fetch only top-level columns for this version
		val topLevelColumns = TableColumnsTable.join(
			VersionColumnTable,
			JoinType.INNER,
			VersionColumnTable.columnDefinitionId,
			TableColumnsTable.id
		)
			.selectAll()
			.where { VersionColumnTable.versionId eq versionId }
			.map { row -> row[TableColumnsTable.id] }

		return topLevelColumns.map { columnId ->
			fetchColumnWithChildren(columnId)
		}
	}

	private fun fetchColumnWithChildren(columnId: UUID): Column {
		val row = TableColumnsTable.selectAll()
			.where { TableColumnsTable.id eq columnId }
			.single()

		// Recursively fetch child columns
		val childIds = TableColumnsTable.selectAll()
			.where { TableColumnsTable.parentColumnId eq columnId }
			.map { it[TableColumnsTable.id] }

		val children = childIds.map { childId ->
			fetchColumnWithChildren(childId)
		}

		return Column(
			id = row[TableColumnsTable.id],
			definition = Column.Definition(
				name = row[TableColumnsTable.name],
				description = row[TableColumnsTable.description],
				type = fromDbType(row[TableColumnsTable.typeName], row[TableColumnsTable.stringFormat]),
				optional = row[TableColumnsTable.optional],
				children = children.map { it.definition }
			),
			children = children
		)
	}

	private fun persistColumnHierarchy(column: Column, parentId: UUID?) {
		TableColumnsTable.upsert(TableColumnsTable.id) { row ->
			row.updateWith(column, parentId)
		}

		// Recursively persist children
		column.children.forEach { child ->
			persistColumnHierarchy(child, parentId = column.id)
		}
	}
}
