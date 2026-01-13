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
}
