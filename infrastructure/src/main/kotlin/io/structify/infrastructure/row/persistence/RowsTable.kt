package io.structify.infrastructure.row.persistence

import io.structify.infrastructure.table.persistence.TableColumnsTable
import io.structify.infrastructure.table.persistence.TableVersionsTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table as ExposedTable

object RowsTable : ExposedTable("rows") {

	val id = uuid("id")
	val versionId = reference("version_id", TableVersionsTable.id, onDelete = ReferenceOption.CASCADE)

	override val primaryKey = PrimaryKey(id)
}

object CellsTable : ExposedTable("cells") {

	val id = long("id").autoIncrement()
	val rowId = reference("row_id", RowsTable.id, onDelete = ReferenceOption.CASCADE)
	val columnDefinitionId = uuid("column_definition_id").references(TableColumnsTable.id)
	val value = text("value")

	override val primaryKey = PrimaryKey(id)

	init {
		uniqueIndex(rowId, columnDefinitionId)
	}
}
