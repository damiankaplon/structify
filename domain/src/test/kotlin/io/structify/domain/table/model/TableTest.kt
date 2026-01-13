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
}
