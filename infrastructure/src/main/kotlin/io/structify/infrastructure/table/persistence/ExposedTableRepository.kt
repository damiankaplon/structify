package io.structify.infrastructure.table.persistence

import io.structify.domain.db.reflection.setPrivateProperty
import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.ColumnDefinition
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Table
import io.structify.domain.table.model.Version
import io.structify.infrastructure.db.NoEntityFoundException
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
			version.columns.forEach { column ->
				TableColumnsTable.upsert(TableColumnsTable.versionId, TableColumnsTable.name) { row ->
					row.updateWith(version.id, column)
				}
			}
			version.removeStaleColumnDefinitions()
		}
		table.removeStaleVersions()
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

	private fun fromDbType(typeName: String, format: String?): ColumnType = when (typeName) {
		"STRING" -> ColumnType.StringType(format = format?.let { StringFormat.valueOf(it) })
		"NUMBER" -> ColumnType.NumberType
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

	private fun UpdateBuilder<*>.updateWith(versionId: UUID, column: ColumnDefinition) {
		this[TableColumnsTable.versionId] = versionId
		this[TableColumnsTable.name] = column.name
		this[TableColumnsTable.description] = column.description
		this[TableColumnsTable.typeName] = toTypeName(column.type)
		this[TableColumnsTable.stringFormat] = toStringFormat(column.type)
		this[TableColumnsTable.optional] = column.optional
	}

	private fun toTypeName(type: ColumnType): String = when (type) {
		is ColumnType.StringType -> "STRING"
		is ColumnType.NumberType -> "NUMBER"
	}

	private fun toStringFormat(type: ColumnType): String? = when (type) {
		is ColumnType.StringType -> type.format?.name
		is ColumnType.NumberType -> null
	}

	private fun Table.removeStaleVersions() {
		val tableId = this.id
		val versionIds = this.versions.map(Version::id)
		TableVersionsTable.deleteWhere {
			(TableVersionsTable.tableId eq tableId) and (TableVersionsTable.id notInList versionIds)
		}
	}

	private fun Version.removeStaleColumnDefinitions() {
		val versionId = this.id
		val columnNames = this.columns.map(ColumnDefinition::name)
		TableColumnsTable.deleteWhere {
			(TableColumnsTable.versionId eq versionId) and (TableColumnsTable.name notInList columnNames)
		}
	}

	private fun fetchColumns(versionId: UUID): List<ColumnDefinition> {
		return TableColumnsTable.selectAll()
			.where { TableColumnsTable.versionId eq versionId }
			.map { row ->
				ColumnDefinition(
					name = row[TableColumnsTable.name],
					description = row[TableColumnsTable.description],
					type = fromDbType(row[TableColumnsTable.typeName], row[TableColumnsTable.stringFormat]),
					optional = row[TableColumnsTable.optional]
				)
			}
	}
}
