package io.structify.domain.table.model

import java.util.UUID

class Table(
	val id: UUID = UUID.randomUUID(),
	val userId: UUID,
	val name: String,
) {

	var versions = emptySet<Version>(); private set

	fun update(definitions: List<Column.Definition>) {
		ColumnHierarchyValidation.validate(definitions)

		val newVersionColumns = definitions.map { definition -> definition.getColumn() }

		val recent: Int = versions.maxOfOrNull(Version::orderNumber) ?: 0
		versions = versions + Version(columns = newVersionColumns, orderNumber = recent + 1)
	}

	private fun Version.flattenColumns(): List<Column> {
		return columns.flatMap { it.flatten() }
	}

	private fun Column.flatten(): List<Column> {
		return listOf(this) + children.flatMap { it.flatten() }
	}

	private fun Column.Definition.getColumn(allColumns: List<Column> = versions.flatMap { it.flattenColumns() }): Column {
		val existing = allColumns.firstOrNull { it.definition == this }
		if (existing != null) {
			return existing
		}
		val children = this.children.map { childDefinition ->
			childDefinition.getColumn(allColumns)
		}
		return Column(
			definition = this,
			children = children
		)
	}

	fun getCurrentVersion(): Version = versions.maxBy { it.orderNumber }

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Table

		return id == other.id
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}
}
