package io.structify.domain.table.model

import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

internal class TableTest {

	@Test
	fun `should change current table version given new version of a table`() {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(
			id = tableId,
			userId = userId,
			name = "People",
		)
		table.update(
			listOf(
				Column.Definition(
					name = "name",
					description = "Person name",
					type = ColumnType.StringType(),
					optional = false
				)
			)
		)

		// when
		table.update(
			listOf(
				Column.Definition(
					name = "name 2",
					description = "Person name 2",
					type = ColumnType.StringType(StringFormat.DATE),
					optional = false
				)
			)
		)

		// then
		val version = table.getCurrentVersion()
		assertThat(version.orderNumber).isEqualTo(2)
		assertThat(version.columns.map(Column::definition)).satisfiesExactlyInAnyOrder(
			{
				assertThat(it.name).isEqualTo("name 2")
				assertThat(it.description).isEqualTo("Person name 2")
				assertThat(it.type).isEqualTo(ColumnType.StringType(StringFormat.DATE))
				assertThat(it.optional).isFalse()
			}
		)
	}

	@Test
	fun `should reuse a column definition from prevoius version given new version having same column`() {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(
			id = tableId,
			userId = userId,
			name = "People",
		)
		table.update(
			listOf(
				Column.Definition(
					name = "birth date",
					description = "Persons birth date",
					type = ColumnType.StringType(StringFormat.DATE),
					optional = false
				)
			)
		)
		val firstVersion = table.getCurrentVersion()
		table.update(
			listOf(
				Column.Definition(
					name = "name",
					description = "Person name",
					type = ColumnType.StringType(),
					optional = false
				)
			)
		)

		// when
		table.update(
			listOf(
				Column.Definition(
					name = "name",
					description = "Person name",
					type = ColumnType.StringType(),
					optional = false
				),
				Column.Definition(
					name = "birth date",
					description = "Persons birth date",
					type = ColumnType.StringType(StringFormat.DATE),
					optional = false
				)
			)
		)

		// then
		val currentVersion = table.getCurrentVersion()
		assertThat(currentVersion.columns).satisfiesExactlyInAnyOrder(
			{
				assertThat(it).isEqualTo(firstVersion.columns.first())
			},
			{
				assertThat(it.definition).isEqualTo(
					Column.Definition(
						name = "name",
						description = "Person name",
						type = ColumnType.StringType(),
						optional = false
					)
				)
			}
		)
	}

	@Test
	fun `should reuse column when definition with children matches`() {
		// given
		val userId = UUID.randomUUID()
		val table = Table(
			userId = userId,
			name = "Test Table",
		)

		val childDef = Column.Definition(
			name = "child",
			description = "Child column",
			type = ColumnType.StringType(),
			optional = false
		)

		val parentDef = Column.Definition(
			name = "parent",
			description = "Parent column",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(childDef)
		)

		// when - create first version with hierarchical structure
		table.update(listOf(parentDef))
		val firstVersion = table.getCurrentVersion()
		val firstParent = firstVersion.columns.first()
		val firstChild = firstParent.children.first()

		// and - create second version with exact same definition
		table.update(listOf(parentDef))
		val secondVersion = table.getCurrentVersion()
		val secondParent = secondVersion.columns.first()
		val secondChild = secondParent.children.first()

		// then - columns should be reused (same IDs)
		assertThat(firstParent.id).isEqualTo(secondParent.id)
		assertThat(firstChild.id).isEqualTo(secondChild.id)
	}

	@Test
	fun `should create new column when child definition changes`() {
		// given
		val userId = UUID.randomUUID()
		val table = Table(
			userId = userId,
			name = "Test Table",
		)

		val oldChildDef = Column.Definition(
			name = "child",
			description = "Old description",
			type = ColumnType.StringType(),
			optional = false
		)

		val oldParentDef = Column.Definition(
			name = "parent",
			description = "Parent column",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(oldChildDef)
		)

		// when - create first version
		table.update(listOf(oldParentDef))
		val firstVersion = table.getCurrentVersion()
		val firstParent = firstVersion.columns.first()
		val firstChild = firstParent.children.first()

		// and - create second version with changed child description
		val newChildDef = Column.Definition(
			name = "child",
			description = "New description",
			type = ColumnType.StringType(),
			optional = false
		)

		val newParentDef = Column.Definition(
			name = "parent",
			description = "Parent column",
			type = ColumnType.ObjectType,
			optional = false,
			children = listOf(newChildDef)
		)

		table.update(listOf(newParentDef))
		val secondVersion = table.getCurrentVersion()
		val secondParent = secondVersion.columns.first()
		val secondChild = secondParent.children.first()

		// then - new columns should be created (different IDs)
		assertThat(firstParent.id).isNotEqualTo(secondParent.id)
		assertThat(firstChild.id).isNotEqualTo(secondChild.id)
		assertThat(secondChild.definition.description).isEqualTo("New description")
	}
}
