package io.structify.domain.table.model

/**
 * Validates hierarchical column definitions according to domain rules:
 * 1. Leaf columns must have STRING or NUMBER type
 * 2. Object columns must have children
 * 3. Unique names within siblings
 * 4. Optional: Maximum nesting depth
 */
object ColumnHierarchyValidation {

	/**
	 * Validates a list of column definitions
	 *
	 * @throws IllegalArgumentException if validation fails
	 */
	fun validate(definitions: List<Column.Definition>) {
		validateUniqueNamesAmongSiblings(definitions)
		definitions.forEach { validateDefinition(it) }
	}

	private fun validateDefinition(definition: Column.Definition) {
		when (definition.type) {
			is ColumnType.ObjectType -> {
				require(definition.children.isNotEmpty()) { "Column '${definition.name}' has OBJECT type but no children. OBJECT columns must have at least one child." }
				validateUniqueNamesAmongSiblings(definition.children)
				definition.children.forEach { validateDefinition(it) }
			}
			else -> {
				require(definition.children.isEmpty()) { "Column '${definition.name}' has type '${definition.type}' but has children." }
			}
		}
	}

	private fun validateUniqueNamesAmongSiblings(siblings: List<Column.Definition>) {
		val duplicates = siblings.groupBy { it.name }.filter { it.value.size > 1 }.keys
		require(duplicates.isEmpty()) {
			"Duplicate column names found at the same level: ${duplicates.joinToString(", ")}"
		}
	}
}
