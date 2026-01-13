package io.structify.infrastructure.table.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table as ExposedTable

object TablesTable : ExposedTable("tables") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

object TableVersionsTable : ExposedTable("table_versions") {
    val id = uuid("id")
    val tableId = reference("table_id", TablesTable.id, onDelete = ReferenceOption.CASCADE)
    val orderNumber = integer("order_number")

    override val primaryKey = PrimaryKey(id)
}

object TableColumnsTable : ExposedTable("table_columns") {

    val id = uuid("id")
    val name = varchar("name", 255)
    val description = text("description")
    val typeName = varchar("type_name", 50)
    val stringFormat = varchar("string_format", 50).nullable()
    val optional = bool("optional")

    override val primaryKey = PrimaryKey(id)
}

object VersionColumnTable : ExposedTable("version_column_assoc") {

    val versionId = reference("version_id", TableVersionsTable.id, onDelete = ReferenceOption.CASCADE)
    val columnDefinitionId = uuid("column_definition_id").references(TableColumnsTable.id)

    override val primaryKey = PrimaryKey(versionId, columnDefinitionId)
}
