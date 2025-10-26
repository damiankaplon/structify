package io.structify.infrastructure.table.persistence

import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.ColumnDefinition
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Table
import io.structify.domain.table.model.Version
import jakarta.inject.Singleton
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.util.UUID

@Singleton
class ExposedTableRepository : TableRepository {

    override suspend fun persist(table: Table): Table {
        TablesTable.upsert(TablesTable.id) { row ->
            assignTableRow(row, table)
        }

        table.versions.forEach { version ->
            persistVersions(table.id, version)
        }
        return requireNotNull(findById(table.userId, table.id))
    }

    override suspend fun findById(userId: UUID, tableId: UUID): Table? {
        val tableRow = TablesTable
            .selectAll()
            .where { (TablesTable.id eq tableId) and (TablesTable.userId eq userId) }
            .firstOrNull() ?: return null

        val versionsRows = TableVersionsTable
            .selectAll()
            .where { TableVersionsTable.tableId eq tableId }
            .toList()

        if (versionsRows.isEmpty()) return null

        val versions = versionsRows.map { vRow ->
            val vId = vRow[TableVersionsTable.id]
            val cols = TableColumnsTable
                .selectAll()
                .where { TableColumnsTable.versionId eq vId }
                .orderBy(TableColumnsTable.id to org.jetbrains.exposed.sql.SortOrder.ASC)
                .map { cRow -> mapColumn(cRow) }
            mapVersion(vRow, cols)
        }

        return mapTable(tableRow, versions)
    }

    private fun toTypeName(type: ColumnType): String = when (type) {
        is ColumnType.StringType -> "STRING"
        is ColumnType.NumberType -> "NUMBER"
    }

    private fun toStringFormat(type: ColumnType): String? = when (type) {
        is ColumnType.StringType -> type.format?.name
        is ColumnType.NumberType -> null
    }

    private fun fromDbType(typeName: String, format: String?): ColumnType = when (typeName) {
        "STRING" -> ColumnType.StringType(format = format?.let { StringFormat.valueOf(it) })
        "NUMBER" -> ColumnType.NumberType
        else -> error("Unknown column type: $typeName")
    }

    private fun persistVersions(tableId: UUID, version: Version) {
        TableVersionsTable.upsert(TableVersionsTable.id) { row ->
            assignVersionRow(row, tableId, version)
        }

        version.columns.forEach { col ->
            persistColumns(version.id, col)
        }
    }

    private fun persistColumns(versionId: UUID, col: ColumnDefinition) {
        TableColumnsTable.upsert(TableColumnsTable.versionId, TableColumnsTable.name) { row ->
            assignColumnRow(row, versionId, col)
        }
    }

    // --- Mapping helpers (Rows -> Domain) ---
    private fun mapColumn(row: ResultRow): ColumnDefinition = ColumnDefinition(
        name = row[TableColumnsTable.name],
        description = row[TableColumnsTable.description],
        type = fromDbType(row[TableColumnsTable.typeName], row[TableColumnsTable.stringFormat]),
        optional = row[TableColumnsTable.optional]
    )

    private fun mapVersion(row: ResultRow, columns: List<ColumnDefinition>): Version = Version(
        id = row[TableVersionsTable.id],
        description = row[TableVersionsTable.description],
        columns = columns,
        orderNumber = row[TableVersionsTable.orderNumber]
    )

    private fun mapTable(tableRow: ResultRow, versions: List<Version>): Table {
        val current = versions.maxBy { it.orderNumber }
        val table = Table(
            id = tableRow[TablesTable.id],
            userId = tableRow[TablesTable.userId],
            name = tableRow[TablesTable.name],
            version = current
        )
        versions.filter { it.id != current.id }.forEach { table.add(it) }
        return table
    }

    // --- Assignment helpers (Domain -> Upsert row) ---
    private fun assignTableRow(row: UpdateBuilder<Int>, table: Table) {
        row[TablesTable.id] = table.id
        row[TablesTable.userId] = table.userId
        row[TablesTable.name] = table.name
    }

    private fun assignVersionRow(row: UpdateBuilder<Int>, tableId: UUID, version: Version) {
        row[TableVersionsTable.id] = version.id
        row[TableVersionsTable.tableId] = tableId
        row[TableVersionsTable.description] = version.description
        row[TableVersionsTable.orderNumber] = version.orderNumber
    }

    private fun assignColumnRow(row: UpdateBuilder<Int>, versionId: UUID, col: ColumnDefinition) {
        row[TableColumnsTable.versionId] = versionId
        row[TableColumnsTable.name] = col.name
        row[TableColumnsTable.description] = col.description
        row[TableColumnsTable.typeName] = toTypeName(col.type)
        row[TableColumnsTable.stringFormat] = toStringFormat(col.type)
        row[TableColumnsTable.optional] = col.optional
    }
}
