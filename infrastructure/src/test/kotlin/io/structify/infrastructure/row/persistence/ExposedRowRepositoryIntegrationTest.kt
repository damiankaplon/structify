package io.structify.infrastructure.row.persistence

import io.structify.domain.row.Cell
import io.structify.domain.row.Row
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
		tableRepo.persist(table)

		val row = Row(
			tableId = tableId,
			cells = linkedSetOf(
				Cell(columnId = 1, value = "value1"),
				Cell(columnId = 2, value = "value2")
			)
		)

		// when
		val persistedRow = rowRepo.save(row)

		// then
		assertThat(persistedRow).isEqualTo(row)

		val foundRows = rowRepo.findByTableId(tableId)
		assertThat(foundRows).hasSize(1)
		val foundRow = foundRows.first()
		assertThat(foundRow.id).isEqualTo(row.id)
		assertThat(foundRow.tableId).isEqualTo(tableId)
		assertThat(foundRow.cells).containsExactlyInAnyOrderElementsOf(row.cells)
	}

	@Test
	fun `should update an existing row with cells`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(id = tableId, userId = userId, name = "Test Table")
		tableRepo.persist(table)

		val rowId = UUID.randomUUID()
		val initialRow = Row(
			id = rowId,
			tableId = tableId,
			cells = linkedSetOf(
				Cell(columnId = 1, value = "initial_value1")
			)
		)
		rowRepo.save(initialRow)

		val updatedRow = Row(
			id = rowId,
			tableId = tableId,
			cells = linkedSetOf(
				Cell(columnId = 1, value = "updated_value1"),
				Cell(columnId = 2, value = "new_value2")
			)
		)

		// when
		rowRepo.save(updatedRow)

		// then
		val foundRows = rowRepo.findByTableId(tableId)
		assertThat(foundRows).hasSize(1)
		val foundRow = foundRows.first()
		assertThat(foundRow.id).isEqualTo(rowId)
		assertThat(foundRow.cells).hasSize(2)
		assertThat(foundRow.cells).containsExactlyInAnyOrderElementsOf(updatedRow.cells)
	}

	@Test
	fun `should remove stale cells when updating a row`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(id = tableId, userId = userId, name = "Test Table")
		tableRepo.persist(table)

		val rowId = UUID.randomUUID()
		val initialRow = Row(
			id = rowId,
			tableId = tableId,
			cells = linkedSetOf(
				Cell(columnId = 1, value = "value1"),
				Cell(columnId = 2, value = "value2")
			)
		)
		rowRepo.save(initialRow)

		val updatedRow = Row(
			id = rowId,
			tableId = tableId,
			cells = linkedSetOf(
				Cell(columnId = 1, value = "updated_value1")
			)
		)

		// when
		rowRepo.save(updatedRow)

		// then
		val foundRows = rowRepo.findByTableId(tableId)
		assertThat(foundRows).hasSize(1)
		val foundRow = foundRows.first()
		assertThat(foundRow.id).isEqualTo(rowId)
		assertThat(foundRow.cells).hasSize(1)
		assertThat(foundRow.cells).containsExactlyInAnyOrderElementsOf(updatedRow.cells)
	}

	@Test
	fun `should find all rows for a table id`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(id = tableId, userId = userId, name = "Test Table")
		tableRepo.persist(table)

		val row1 = Row(tableId = tableId, cells = linkedSetOf(Cell(1, "r1c1")))
		val row2 = Row(tableId = tableId, cells = linkedSetOf(Cell(1, "r2c1")))
		rowRepo.save(row1)
		rowRepo.save(row2)

		// when
		val foundRows = rowRepo.findByTableId(tableId)

		// then
		assertThat(foundRows).hasSize(2)
		assertThat(foundRows.map { it.id }).containsExactlyInAnyOrder(row1.id, row2.id)
	}

	@Test
	fun `should return empty set when no rows found for table id`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()
		val table = Table(id = tableId, userId = userId, name = "Test Table")
		tableRepo.persist(table)

		// when
		val foundRows = rowRepo.findByTableId(tableId)

		// then
		assertThat(foundRows).isEmpty()
	}
}
