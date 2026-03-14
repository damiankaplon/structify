package io.structify.domain.row

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class CellHierarchyTest {

	@Test
	fun `should create cell with nested children`() {
		val childCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "John"
		)

		val parentCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "",
			children = setOf(childCell)
		)

		assertEquals(1, parentCell.children.size)
		assertEquals("John", parentCell.children.first().value)
	}

	@Test
	fun `should create deeply nested cells`() {
		val nameCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "John"
		)

		val ageCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "30"
		)

		val streetCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "123 Main St"
		)

		val cityCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "New York"
		)

		val addressCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "",
			children = setOf(streetCell, cityCell)
		)

		val personCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "",
			children = setOf(nameCell, ageCell, addressCell)
		)

		assertEquals(3, personCell.children.size)

		val address = personCell.children.find { it.columnDefinitionId == addressCell.columnDefinitionId }
		assertNotNull(address)
		assertEquals(2, address!!.children.size)
	}

	@Test
	fun `should support mixed hierarchy with leaf and object cells`() {
		val firstName = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "John"
		)

		val lastName = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "Doe"
		)

		val age = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "30"
		)

		val street = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "123 Main St"
		)

		val city = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "New York"
		)

		val addressCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "",
			children = setOf(street, city)
		)

		val personCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "",
			children = setOf(firstName, lastName, age, addressCell)
		)

		// Person has 4 direct children
		assertEquals(4, personCell.children.size)

		// Address has 2 children
		val address = personCell.children.find { it.columnDefinitionId == addressCell.columnDefinitionId }
		assertEquals(2, address!!.children.size)

		// Leaf cells have no children
		val name = personCell.children.find { it.columnDefinitionId == firstName.columnDefinitionId }
		assertEquals(0, name!!.children.size)
	}

	@Test
	fun `should handle empty children set for leaf cells`() {
		val leafCell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "some value",
			children = emptySet()
		)

		assertEquals(0, leafCell.children.size)
		assertTrue(leafCell.children.isEmpty())
	}

	@Test
	fun `should allow value modification in mutable cell`() {
		val cell = Cell(
			columnDefinitionId = UUID.randomUUID(),
			value = "initial",
			children = emptySet()
		)

		assertEquals("initial", cell.value)

		cell.value = "updated"
		assertEquals("updated", cell.value)
	}
}
