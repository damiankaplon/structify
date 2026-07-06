package io.structify.infrastructure.table.persistence

import io.structify.domain.db.reflection.setPrivateProperty
import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.*
import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.Table
import io.structify.infrastructure.db.NoEntityFoundException
import io.structify.infrastructure.db.OptimisticLockException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.util.*

class ExposedTableRepository : TableRepository {

	override suspend fun persist(table: Table): Table {
        persistRoot(table)

        // Versions are append-only and a persisted version's columns never change,
        // so we only insert versions that aren't stored yet and leave the rest
        // untouched. Optimistic locking on the root guards concurrent appends.
        val storedVersionIds = TableVersionsTable
            .select(TableVersionsTable.id)
            .where { TableVersionsTable.tableId eq table.id }
            .mapTo(mutableSetOf()) { it[TableVersionsTable.id] }

        table.versions
            .filterNot { it.id in storedVersionIds }
            .forEach { version ->
                TableVersionsTable.insert { row -> row.updateWith(table.id, version) }

                // Column definitions are immutable and shared across versions, so
                // insert-if-absent; the FK from the assoc row keeps them reachable.
                version.columns.forEach { column ->
                    persistColumnHierarchy(column, parentId = null)
                }

                // Only top-level columns are associated to the version directly.
                version.columns.forEach { column ->
                    VersionColumnTable.insert { row ->
                        row[VersionColumnTable.versionId] = version.id
                        row[VersionColumnTable.columnDefinitionId] = column.id
                    }
                }
            }
		return table
	}

    /**
     * Optimistically inserts a new table or updates the existing one, guarding
     * against concurrent modifications via the [TablesTable.optLock] counter.
     */
    private fun persistRoot(table: Table) {
        val updated =
            TablesTable.update({ (TablesTable.id eq table.id) and (TablesTable.optLock eq table.optLock) }) { row ->
                row[TablesTable.userId] = table.userId
                row[TablesTable.name] = table.name
                row[TablesTable.optLock] = table.optLock + 1
            }
        if (updated == 0) {
            val exists = !TablesTable.selectAll().where { TablesTable.id eq table.id }.empty()
            if (exists) throw OptimisticLockException("table", table.id)
            TablesTable.insert { row ->
                row.updateWith(table)
                row[TablesTable.optLock] = 0
            }
        } else {
            table.optLock += 1
        }
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
                    optLock = row[TablesTable.optLock],
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
                    optLock = row[TablesTable.optLock],
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
        // Immutable definition that may already be stored from an earlier version.
        TableColumnsTable.insertIgnore { row ->
			row.updateWith(column, parentId)
		}

		// Recursively persist children
		column.children.forEach { child ->
			persistColumnHierarchy(child, parentId = column.id)
		}
	}
}
