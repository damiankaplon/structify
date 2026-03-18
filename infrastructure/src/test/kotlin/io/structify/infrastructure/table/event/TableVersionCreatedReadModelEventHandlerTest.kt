package io.structify.infrastructure.table.event

import io.structify.domain.table.TableVersionCreated
import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Version
import io.structify.infrastructure.table.readmodel.VersionReadModelInMemoryRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.util.*
import kotlin.test.Test

internal class TableVersionCreatedReadModelEventHandlerTest {

	private val versionReadModelRepository = VersionReadModelInMemoryRepository()
	private val handler = TableVersionCreatedReadModelEventHandler(versionReadModelRepository)

	@Test
	fun `should project version into read model when TableVersionCreated event is handled`() {
		runBlocking {
			// given
			val tableId = UUID.randomUUID()
			val userId = UUID.randomUUID()
			val versionId = UUID.randomUUID()

			val version = Version(
				id = versionId,
				columns = listOf(
					Column(
						definition = Column.Definition(
							name = "email",
							description = "User email",
							type = ColumnType.StringType(format = null),
							optional = false,
						)
					)
				),
				orderNumber = 1,
			)

			val event = TableVersionCreated(tableId = tableId, userId = userId, version = version)

			// when
			handler.handle(event)

			// then
			val stored = versionReadModelRepository.findCurrentVersionByTableId(userId, tableId)
			assertThat(stored).isNotNull
			assertThat(stored!!.id).isEqualTo(versionId.toString())
			assertThat(stored.orderNumber).isEqualTo(1)
			assertThat(stored.columns).hasSize(1)
			assertThat(stored.columns.first().name).isEqualTo("email")
			assertThat(stored.columns.first().type.type).isEqualTo("STRING")
			assertThat(stored.columns.first().type.format).isNull()
			assertThat(stored.columns.first().optional).isFalse()
		}
	}

	@Test
	fun `should project multiple versions accumulating in read model`() {
		runBlocking {
			// given
			val tableId = UUID.randomUUID()
			val userId = UUID.randomUUID()

			val colDef = Column.Definition(
				name = "name",
				description = "Full name",
				type = ColumnType.StringType(format = StringFormat.DATE),
				optional = false,
			)

			val version1 = Version(columns = listOf(Column(definition = colDef)), orderNumber = 1)
			val version2 = Version(
				columns = listOf(
					Column(definition = colDef),
					Column(
						definition = Column.Definition(
							name = "age",
							description = "Age",
							type = ColumnType.NumberType,
							optional = true
						)
					)
				),
				orderNumber = 2
			)

			// when
			handler.handle(TableVersionCreated(tableId = tableId, userId = userId, version = version1))
			handler.handle(TableVersionCreated(tableId = tableId, userId = userId, version = version2))

			// then
			val all = versionReadModelRepository.findAllVersionsByTableId(userId, tableId)
			assertThat(all).hasSize(2)

			val current = versionReadModelRepository.findCurrentVersionByTableId(userId, tableId)
			assertThat(current!!.orderNumber).isEqualTo(2)
			assertThat(current.columns).hasSize(2)
			assertThat(current.columns.map { it.name }).containsExactlyInAnyOrder("name", "age")
		}
	}
}
