package io.structify.domain.table.model

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

internal class ColumnHierarchyValidationTest {

	@Test
	fun `should create column definition with children`() {
		val definition = Column.Definition(
			name = "Person",
			description = "A person",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(
				Column.Definition(
					name = "firstName",
					description = "First name",
					type = ColumnType.StringType(),
					optional = false
				)
			)
		)

		assertEquals("Person", definition.name)
		assertEquals(1, definition.children.size)
		assertEquals("firstName", definition.children[0].name)
	}

	@Test
	fun `should consider definitions equal when children match`() {
		val childDef = Column.Definition(
			name = "firstName",
			description = "First name",
			type = ColumnType.StringType(),
			optional = false
		)

		val def1 = Column.Definition(
			name = "Person",
			description = "A person",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(childDef)
		)

		val def2 = Column.Definition(
			name = "Person",
			description = "A person",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(childDef)
		)

		assertEquals(def1, def2)
	}

	@Test
	fun `should not consider definitions equal when children differ`() {
		val def1 = Column.Definition(
			name = "Person",
			description = "A person",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(
				Column.Definition(
					name = "firstName",
					description = "First name",
					type = ColumnType.StringType(),
					optional = false
				)
			)
		)

		val def2 = Column.Definition(
			name = "Person",
			description = "A person",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(
				Column.Definition(
					name = "lastName",
					description = "Last name",
					type = ColumnType.StringType(),
					optional = false
				)
			)
		)

		assertNotEquals(def1, def2)
	}

	@Test
	fun `should enforce OBJECT type for columns with children`() {
		val definitions = listOf(
			Column.Definition(
				name = "Invalid",
				description = "Invalid column",
				type = ColumnType.StringType(),
				optional = false,
				children = listOf(
					Column.Definition(
						name = "child",
						description = "Child",
						type = ColumnType.StringType(),
						optional = false
					)
				)
			)
		)

		assertThrows<IllegalArgumentException> {
			ColumnHierarchyValidation.validate(definitions)
		}
	}

	@Test
	fun `should reject STRING type for columns with children`() {
		val definitions = listOf(
			Column.Definition(
				name = "Invalid",
				description = "Invalid column",
				type = ColumnType.NumberType,
				optional = false,
				children = listOf(
					Column.Definition(
						name = "child",
						description = "Child",
						type = ColumnType.StringType(),
						optional = false
					)
				)
			)
		)

		assertThrows<IllegalArgumentException> {
			ColumnHierarchyValidation.validate(definitions)
		}
	}

	@Test
	fun `should reject OBJECT type without children`() {
		val definitions = listOf(
			Column.Definition(
				name = "Invalid",
				description = "Invalid column",
				type = ColumnType.ObjectType,
				optional = false,
				children = emptyList()
			)
		)

		val exception = assertThrows<IllegalArgumentException> {
			ColumnHierarchyValidation.validate(definitions)
		}

		assertTrue(exception.message!!.contains("has OBJECT type but no children"))
	}

	@Test
	fun `should validate unique names among siblings`() {
		val definitions = listOf(
			Column.Definition(
				name = "Duplicate",
				description = "First",
				type = ColumnType.StringType(),
				optional = false
			),
			Column.Definition(
				name = "Duplicate",
				description = "Second",
				type = ColumnType.StringType(),
				optional = false
			)
		)

		val exception = assertThrows<IllegalArgumentException> {
			ColumnHierarchyValidation.validate(definitions)
		}

		assertTrue(exception.message!!.contains("Duplicate column names found"))
	}

	@Test
	fun `should allow same name at different tree levels`() {
		val definitions = listOf(
			Column.Definition(
				name = "name",
				description = "Top level name",
				type = ColumnType.StringType(),
				optional = false
			),
			Column.Definition(
				name = "Person",
				description = "A person",
				type = ColumnType.ObjectType,
				optional = false,
				children = listOf(
					Column.Definition(
						name = "name",
						description = "Person's name",
						type = ColumnType.StringType(),
						optional = false
					)
				)
			)
		)

		// Should not throw
		assertDoesNotThrow {
			ColumnHierarchyValidation.validate(definitions)
		}
	}
}
