package io.structify.domain.table.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import java.util.UUID
import kotlin.test.Test

internal class TableHierarchicalVersionTest {

	@Test
	fun `should create version with hierarchical columns`() {
		val table = Table(
			userId = UUID.randomUUID(),
			name = "Test Table"
		)

		val definitions = listOf(
			Column.Definition(
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
					),
					Column.Definition(
						name = "age",
						description = "Age",
						type = ColumnType.NumberType,
						optional = false
					)
				)
			)
		)

		table.update(definitions)

		val version = table.getCurrentVersion()
		assertEquals(1, version.orderNumber)
		assertEquals(1, version.columns.size)

		val personColumn = version.columns[0]
		assertEquals("Person", personColumn.definition.name)
		assertEquals(2, personColumn.children.size)
		assertEquals("firstName", personColumn.children[0].definition.name)
		assertEquals("age", personColumn.children[1].definition.name)
	}

	@Test
	fun `should reuse column when definition with children matches`() {
		val table = Table(
			userId = UUID.randomUUID(),
			name = "Test Table"
		)

		val definitions = listOf(
			Column.Definition(
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
		)

		// Create first version
		table.update(definitions)
		val firstVersion = table.getCurrentVersion()
		val firstPersonId = firstVersion.columns[0].id
		val firstChildId = firstVersion.columns[0].children[0].id

		// Create second version with same definitions
		table.update(definitions)
		val secondVersion = table.getCurrentVersion()
		val secondPersonId = secondVersion.columns[0].id
		val secondChildId = secondVersion.columns[0].children[0].id

		// IDs should be reused
		assertEquals(firstPersonId, secondPersonId)
		assertEquals(firstChildId, secondChildId)
	}

	@Test
	fun `should create new column when child definition changes`() {
		val table = Table(
			userId = UUID.randomUUID(),
			name = "Test Table"
		)

		val firstDefinitions = listOf(
			Column.Definition(
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
		)

		// Create first version
		table.update(firstDefinitions)
		val firstVersion = table.getCurrentVersion()
		val firstPersonId = firstVersion.columns[0].id

		// Create second version with modified child
		val secondDefinitions = listOf(
			Column.Definition(
				name = "Person",
				description = "A person",
				type = ColumnType.ObjectType,
				optional = false,
				children = listOf(
					Column.Definition(
						name = "firstName",
						description = "Updated description",
						type = ColumnType.StringType(),
						optional = false
					)
				)
			)
		)

		table.update(secondDefinitions)
		val secondVersion = table.getCurrentVersion()
		val secondPersonId = secondVersion.columns[0].id

		// Parent ID should be different because child changed
		assertNotEquals(firstPersonId, secondPersonId)
	}

	@Test
	fun `should handle deeply nested structures`() {
		val table = Table(
			userId = UUID.randomUUID(),
			name = "Test Table"
		)

		val definitions = listOf(
			Column.Definition(
				name = "Company",
				description = "A company",
				type = ColumnType.ObjectType,
				optional = false,
				children = listOf(
					Column.Definition(
						name = "name",
						description = "Company name",
						type = ColumnType.StringType(),
						optional = false
					),
					Column.Definition(
						name = "address",
						description = "Company address",
						type = ColumnType.ObjectType,
						optional = false,
						children = listOf(
							Column.Definition(
								name = "street",
								description = "Street",
								type = ColumnType.StringType(),
								optional = false
							),
							Column.Definition(
								name = "city",
								description = "City",
								type = ColumnType.StringType(),
								optional = false
							),
							Column.Definition(
								name = "country",
								description = "Country",
								type = ColumnType.ObjectType,
								optional = false,
								children = listOf(
									Column.Definition(
										name = "code",
										description = "Country code",
										type = ColumnType.StringType(),
										optional = false
									),
									Column.Definition(
										name = "name",
										description = "Country name",
										type = ColumnType.StringType(),
										optional = false
									)
								)
							)
						)
					)
				)
			)
		)

		table.update(definitions)

		val version = table.getCurrentVersion()
		val companyColumn = version.columns[0]
		assertEquals("Company", companyColumn.definition.name)
		assertEquals(2, companyColumn.children.size)

		val addressColumn = companyColumn.children[1]
		assertEquals("address", addressColumn.definition.name)
		assertEquals(3, addressColumn.children.size)

		val countryColumn = addressColumn.children[2]
		assertEquals("country", countryColumn.definition.name)
		assertEquals(2, countryColumn.children.size)
		assertEquals("code", countryColumn.children[0].definition.name)
		assertEquals("name", countryColumn.children[1].definition.name)
	}

	@Test
	fun `should reject invalid hierarchy structure`() {
		val table = Table(
			userId = UUID.randomUUID(),
			name = "Test Table"
		)

		val invalidDefinitions = listOf(
			Column.Definition(
				name = "Invalid",
				description = "Invalid column",
				type = ColumnType.StringType(), // String type with children
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

		assertThrows(IllegalArgumentException::class.java) {
			table.update(invalidDefinitions)
		}
	}
}
