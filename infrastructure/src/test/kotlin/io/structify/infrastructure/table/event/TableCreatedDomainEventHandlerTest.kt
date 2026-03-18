package io.structify.infrastructure.table.event

import io.structify.domain.table.TableCreated
import io.structify.domain.test.fixtures.table.TableInMemoryRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.util.*
import kotlin.test.Test

internal class TableCreatedDomainEventHandlerTest {

	private val tableRepository = TableInMemoryRepository()
	private val handler = TableCreatedDomainEventHandler(tableRepository)

	@Test
	fun `should create domain Table aggregate when TableCreated event is handled`() {
		runBlocking {
			// given
			val tableId = UUID.randomUUID()
			val userId = UUID.randomUUID()
			val event = TableCreated(
				tableId = tableId,
				userId = userId,
				name = "My Table",
			)

			// when
			handler.handle(event)

			// then
			val table = tableRepository.findByIdThrow(userId, tableId)
			assertThat(table.id).isEqualTo(tableId)
			assertThat(table.userId).isEqualTo(userId)
			assertThat(table.name).isEqualTo("My Table")
			assertThat(table.versions).isEmpty()
		}
	}

	@Test
	fun `should persist table so it is accessible by user id`() {
		runBlocking {
			// given
			val tableId = UUID.randomUUID()
			val userId = UUID.randomUUID()
			val otherUserId = UUID.randomUUID()

			handler.handle(TableCreated(tableId = tableId, userId = userId, name = "Restricted Table"))

			// when / then – same user can find it
			val found = tableRepository.findById(userId, tableId)
			assertThat(found).isNotNull

			// other user cannot find it
			val notFound = tableRepository.findById(otherUserId, tableId)
			assertThat(notFound).isNull()
		}
	}
}
