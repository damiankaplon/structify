package io.structify.infrastructure.table.readmodel.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table as ExposedTable

object TableReadModelTable : ExposedTable("table_read_model") {
	val id = uuid("id")
	val userId = uuid("user_id")
	val name = varchar("name", 255)
	val description = text("description").default("")

	override val primaryKey = PrimaryKey(id)
}

object VersionReadModelTable : ExposedTable("version_read_model") {
	val id = uuid("id")
	val tableId = reference("table_id", TableReadModelTable.id, onDelete = ReferenceOption.CASCADE)
	val orderNumber = integer("order_number")

	override val primaryKey = PrimaryKey(id)
}

object ColumnReadModelTable : ExposedTable("column_read_model") {
	val id = uuid("id")
	val name = varchar("name", 255)
	val description = text("description")
	val typeName = varchar("type_name", 50)
	val stringFormat = varchar("string_format", 50).nullable()
	val optional = bool("optional")
	val parentColumnId = uuid("parent_column_id").nullable()

	override val primaryKey = PrimaryKey(id)
}

object VersionColumnReadModelTable : ExposedTable("version_column_read_model") {
	val versionId = reference("version_id", VersionReadModelTable.id, onDelete = ReferenceOption.CASCADE)
	val columnDefinitionId = uuid("column_definition_id").references(ColumnReadModelTable.id)

	override val primaryKey = PrimaryKey(versionId, columnDefinitionId)
}
