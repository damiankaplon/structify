package io.structify.infrastructure.row.persistence

import io.structify.infrastructure.table.persistence.TablesTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table as ExposedTable

object RowsTable : ExposedTable("rows") {

	val id = uuid("id")
	val tableId = reference("table_id", TablesTable.id, onDelete = ReferenceOption.CASCADE)

	override val primaryKey = PrimaryKey(id)
}

object CellsTable : ExposedTable("cells") {

	val id = long("id").autoIncrement()
	val rowId = reference("row_id", RowsTable.id, onDelete = ReferenceOption.CASCADE)
	val columnId = integer("column_id")
	val value = text("value")

	init {
		uniqueIndex("cells_row_id_column_id_uk", rowId, columnId)
	}

	override val primaryKey = PrimaryKey(id)
}
