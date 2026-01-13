package io.structify.domain.table.model

import java.util.UUID

class Table(
	val id: UUID = UUID.randomUUID(),
	val userId: UUID,
	val name: String,
) {

	var versions = emptySet<Version>(); private set

	fun update(definitions: List<Column.Definition>) {
		val recent: Int = versions.maxOfOrNull(Version::orderNumber) ?: 0
		val allColumns = versions.flatMap(Version::columns)
		val newVersionColumns = definitions.map { definition ->
			allColumns.firstOrNull { it.definition == definition } ?: Column(definition = definition)
		}
		versions = versions + Version(columns = newVersionColumns, orderNumber = recent + 1)
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
