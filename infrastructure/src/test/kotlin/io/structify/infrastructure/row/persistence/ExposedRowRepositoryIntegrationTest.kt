package io.structify.infrastructure.row.persistence

import io.structify.domain.row.Cell
import io.structify.domain.row.Row
import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.Table
import io.structify.infrastructure.table.persistence.ExposedTableRepository
import io.structify.infrastructure.test.DatabaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

internal class ExposedRowRepositoryIntegrationTest : DatabaseIntegrationTest() {

	private val tableRepo = ExposedTableRepository()
	private val rowRepo = ExposedRowRepository()

	@Test
	fun `should save a new row with cells`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(id = tableId, userId = userId, name = "Test Table")
		table.update(
			listOf(
				Column.Definition(
					name = "column1",
					description = "",
					type = ColumnType.StringType(),
					optional = false
				),
				Column.Definition(
					name = "column2",
					description = "",
					type = ColumnType.StringType(),
					optional = false
				)
			)
		)
		tableRepo.persist(table)
		val column1 = table.getCurrentVersion().columns.first()
		val column2 = table.getCurrentVersion().columns.last()

		val row = Row(
			versionId = table.getCurrentVersion().id,
			cells = linkedSetOf(
				Cell(column1.id, value = "value1"),
				Cell(column2.id, value = "value2")
			)
		)

		// when
		val persistedRow = rowRepo.save(row)

		// then
		assertThat(persistedRow).isEqualTo(row)

		val foundRow = rowRepo.findByIdOrThrow(row.id)
		assertThat(foundRow.id).isEqualTo(row.id)
		assertThat(foundRow.versionId).isEqualTo(table.getCurrentVersion().id)
		assertThat(foundRow.cells).containsExactlyInAnyOrderElementsOf(row.cells)
	}

	@Test
	fun `should update an existing row with cells`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(id = tableId, userId = userId, name = "Test Table")
		table.update(
			listOf(
				Column.Definition(
					name = "column1",
					description = "",
					type = ColumnType.StringType(),
					optional = false
				),
			)
		)
		tableRepo.persist(table)
		val column1 = table.getCurrentVersion().columns.first()

		val rowId = UUID.randomUUID()
		val initialRow = Row(
			id = rowId,
			versionId = table.getCurrentVersion().id,
			cells = linkedSetOf(
				Cell(columnDefinitionId = column1.id, value = "initial_value1")
			)
		)
		rowRepo.save(initialRow)

		val updatedRow = Row(
			id = rowId,
			versionId = table.getCurrentVersion().id,
			cells = linkedSetOf(
				Cell(columnDefinitionId = column1.id, value = "updated_value1"),
			)
		)

		// when
		rowRepo.save(updatedRow)

		// then
		val foundRow = rowRepo.findByIdOrThrow(rowId)
		assertThat(foundRow.id).isEqualTo(rowId)
		assertThat(foundRow.cells).hasSize(1)
		assertThat(foundRow.cells).containsExactlyInAnyOrderElementsOf(updatedRow.cells)
	}

	@Test
	fun `should save and load row with complex nested cell hierarchy`() = rollbackTransaction {
		// given - create a table with nested column structure
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(id = tableId, userId = userId, name = "Company Table")

		table.update(
			listOf(
				// Root level string column
				Column.Definition(
					name = "companyName",
					description = "The name of the company",
					type = ColumnType.StringType(),
					optional = false
				),
				// Root level object column with nested structure
				Column.Definition(
					name = "headquarters",
					description = "Company headquarters information",
					type = ColumnType.ObjectType,
					optional = false,
					children = listOf(
						Column.Definition(
							name = "city",
							description = "City where HQ is located",
							type = ColumnType.StringType(),
							optional = false
						),
						// Nested object column (2 levels deep)
						Column.Definition(
							name = "contact",
							description = "Contact information",
							type = ColumnType.ObjectType,
							optional = true,
							children = listOf(
								Column.Definition(
									name = "email",
									description = "Contact email",
									type = ColumnType.StringType(),
									optional = false
								),
								Column.Definition(
									name = "phone",
									description = "Contact phone",
									type = ColumnType.StringType(),
									optional = false
								)
							)
						)
					)
				)
			)
		)
		tableRepo.persist(table)

		// Get column references
		val version = table.getCurrentVersion()
		val companyNameCol = version.columns.first { it.definition.name == "companyName" }
		val headquartersCol = version.columns.first { it.definition.name == "headquarters" }
		val cityCol = headquartersCol.children.first { it.definition.name == "city" }
		val contactCol = headquartersCol.children.first { it.definition.name == "contact" }
		val emailCol = contactCol.children.first { it.definition.name == "email" }
		val phoneCol = contactCol.children.first { it.definition.name == "phone" }

		// Create row with nested cell structure
		val row = Row(
			versionId = version.id,
			cells = linkedSetOf(
				// Root level string cell
				Cell(
					columnDefinitionId = companyNameCol.id,
					value = "Acme Corporation"
				),
				// Root level object cell with nested children
				Cell(
					columnDefinitionId = headquartersCol.id,
					value = "",
					children = setOf(
						// 1st level nested cells
						Cell(
							columnDefinitionId = cityCol.id,
							value = "San Francisco"
						),
						// 1st level nested object cell with children
						Cell(
							columnDefinitionId = contactCol.id,
							value = "",
							children = setOf(
								// 2nd level nested cells
								Cell(
									columnDefinitionId = emailCol.id,
									value = "contact@acme.com"
								),
								Cell(
									columnDefinitionId = phoneCol.id,
									value = "+1-555-0123"
								)
							)
						)
					)
				)
			)
		)

		// when
		val persistedRow = rowRepo.save(row)

		// then
		assertThat(persistedRow).isEqualTo(row)

		val loadedRow = rowRepo.findByIdOrThrow(row.id)

		// Verify row metadata
		assertThat(loadedRow.id).isEqualTo(row.id)
		assertThat(loadedRow.versionId).isEqualTo(version.id)

		// Verify root level cells
		assertThat(loadedRow.cells).hasSize(2)

		// Verify root level string cell
		val loadedCompanyNameCell = loadedRow.cells.first { it.columnDefinitionId == companyNameCol.id }
		assertThat(loadedCompanyNameCell.value).isEqualTo("Acme Corporation")
		assertThat(loadedCompanyNameCell.children).isEmpty()

		// Verify root level object cell
		val loadedHeadquartersCell = loadedRow.cells.first { it.columnDefinitionId == headquartersCol.id }
		assertThat(loadedHeadquartersCell.value).isEqualTo("")
		assertThat(loadedHeadquartersCell.children).hasSize(2)

		// Verify 1st level nested cells
		val loadedCityCell = loadedHeadquartersCell.children.first { it.columnDefinitionId == cityCol.id }
		assertThat(loadedCityCell.value).isEqualTo("San Francisco")
		assertThat(loadedCityCell.children).isEmpty()

		val loadedContactCell = loadedHeadquartersCell.children.first { it.columnDefinitionId == contactCol.id }
		assertThat(loadedContactCell.value).isEqualTo("")
		assertThat(loadedContactCell.children).hasSize(2)

		// Verify 2nd level nested cells
		val loadedEmailCell = loadedContactCell.children.first { it.columnDefinitionId == emailCol.id }
		assertThat(loadedEmailCell.value).isEqualTo("contact@acme.com")
		assertThat(loadedEmailCell.children).isEmpty()

		val loadedPhoneCell = loadedContactCell.children.first { it.columnDefinitionId == phoneCol.id }
		assertThat(loadedPhoneCell.value).isEqualTo("+1-555-0123")
		assertThat(loadedPhoneCell.children).isEmpty()
	}
}
