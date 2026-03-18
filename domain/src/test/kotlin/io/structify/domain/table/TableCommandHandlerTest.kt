package io.structify.domain.table

import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.Table
import io.structify.domain.test.fixtures.table.TableInMemoryRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.util.*
import kotlin.test.Test

internal class TableCommandHandlerTest {

	private val tableRepository = TableInMemoryRepository()
	private val handler = TableCommandHandler(tableRepository)

	@Test
	fun `should create a new version for existing table and return TableVersionCreated event`() {
		runBlocking {
			// given
			val userId = UUID.randomUUID()
			val tableId = UUID.randomUUID()
			val table = Table(id = tableId, userId = userId, name = "People")
			tableRepository.persist(table)

			val command = CreateTableVersionCommand(
				userId = userId,
				tableId = tableId,
				columns = listOf(
					Column.Definition(
						name = "name",
						description = "Person name",
						type = ColumnType.StringType(),
						optional = false,
					)
				)
			)

			// when
			val event = handler.handle(command)

			// then
			assertThat(event.tableId).isEqualTo(tableId)
			assertThat(event.userId).isEqualTo(userId)
			assertThat(event.version.orderNumber).isEqualTo(1)
			assertThat(event.version.columns).hasSize(1)
			assertThat(event.version.columns.first().definition.name).isEqualTo("name")
		}
	}

	@Test
	fun `should persist the updated table aggregate after version creation`() {
		runBlocking {
			// given
			val userId = UUID.randomUUID()
			val tableId = UUID.randomUUID()
			val table = Table(id = tableId, userId = userId, name = "Orders")
			tableRepository.persist(table)

			val command = CreateTableVersionCommand(
				userId = userId,
				tableId = tableId,
				columns = listOf(
					Column.Definition(
						name = "orderId",
						description = "Order identifier",
						type = ColumnType.StringType(),
						optional = false,
					)
				)
			)

			// when
			handler.handle(command)

			// then
			val persisted = tableRepository.findByIdThrow(userId, tableId)
			assertThat(persisted.versions).hasSize(1)
			assertThat(persisted.versions.first().columns.first().definition.name).isEqualTo("orderId")
		}
	}

	@Test
	fun `should throw when table does not exist for user`() {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()

		val command = CreateTableVersionCommand(
			userId = userId,
			tableId = tableId,
			columns = listOf(
				Column.Definition(
					name = "col",
					description = "desc",
					type = ColumnType.NumberType,
					optional = false,
				)
			)
		)

		// when / then
		assertThatThrownBy {
			runBlocking { handler.handle(command) }
		}.isInstanceOf(NoSuchElementException::class.java)
	}

	@Test
	fun `should increment version orderNumber on each version creation`() {
		runBlocking {
			// given
			val userId = UUID.randomUUID()
			val tableId = UUID.randomUUID()
			val table = Table(id = tableId, userId = userId, name = "Employees")
			tableRepository.persist(table)

			val colDef = Column.Definition(
				name = "email",
				description = "Employee email",
				type = ColumnType.StringType(),
				optional = false,
			)

			// when
			handler.handle(CreateTableVersionCommand(userId = userId, tableId = tableId, columns = listOf(colDef)))
			val event2 =
				handler.handle(CreateTableVersionCommand(userId = userId, tableId = tableId, columns = listOf(colDef)))

			// then
			assertThat(event2.version.orderNumber).isEqualTo(2)
		}
	}
}
