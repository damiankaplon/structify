package io.structify.infrastructure.table.persistence

import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Table
import io.structify.domain.table.model.Version
import io.structify.infrastructure.test.DatabaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

internal class ExposedTableRepositoryIntegrationTest : DatabaseIntegrationTest() {

	private val repo = ExposedTableRepository()

	@Test
	fun `should persist table aggregate into database`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()

		val table = Table(
			id = tableId,
			userId = userId,
			name = "People",
		).apply {
			update(
				listOf(
					Column.Definition(
						name = "name",
						description = "Person name",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					),
				)
			)
		}

		// when
		val persisted = repo.persist(table)

		// then
		assertThat(persisted).isEqualTo(table)

		val found = repo.findById(userId, tableId)
		assertThat(found).isNotNull
		val loaded = found!!

		// table aggregate
		assertThat(loaded.id).isEqualTo(tableId)
		assertThat(loaded.userId).isEqualTo(userId)
		assertThat(loaded.name).isEqualTo("People")

		// versions
		assertThat(loaded.versions).hasSize(1)
		val loadedV1 = loaded.versions.first()
		assertThat(loadedV1.orderNumber).isEqualTo(1)

		// columns
		assertThat(loadedV1.columns).hasSize(1)
		val nameCol = loadedV1.columns.first { it.definition.name == "name" }
		assertThat(nameCol.definition.description).isEqualTo("Person name")
		assertThat(nameCol.definition.type).isEqualTo(ColumnType.StringType(format = StringFormat.DATE))
		assertThat(nameCol.definition.optional).isFalse()
	}

	@Test
	fun `should upsert existing records when persisting same aggregate again`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()

		val columnDefintion1 = Column.Definition(
			name = "name", // same pk for columns (versionId+name)
			description = "name v2",
			type = ColumnType.StringType(format = StringFormat.DATE),
			optional = true
		)
		val table = Table(
			id = tableId,
			userId = userId,
			name = "People",
		).apply {
			update(listOf(columnDefintion1))
			repo.persist(this)
		}

		val columnDefinition2 = Column.Definition(
			name = "age",
			description = "age added",
			type = ColumnType.NumberType,
			optional = false
		)
		table.update(
			listOf(
				Column.Definition(
					name = "name",
					description = "name v2",
					type = ColumnType.StringType(format = StringFormat.DATE),
					optional = true
				),
				columnDefinition2
			),
		)
		repo.persist(table)

		// then
		val found = repo.findById(userId, tableId)
		assertThat(found).isNotNull

		assertThat(found!!.name).isEqualTo("People")

		// versions
		assertThat(found.versions).hasSize(2)
		val firstVersion = found.versions.minBy(Version::orderNumber)
		assertThat(firstVersion.orderNumber).isEqualTo(1)
		assertThat(firstVersion.columns.map(Column::definition)).containsExactlyInAnyOrder(columnDefintion1)
		val firstVersionColumns = firstVersion.columns

		val secondVersion = found.versions.maxBy(Version::orderNumber)
		assertThat(secondVersion.orderNumber).isEqualTo(2)
		assertThat(secondVersion.columns).containsAll(firstVersionColumns)
		assertThat(secondVersion.columns.map(Column::definition)).containsExactlyInAnyOrder(columnDefintion1, columnDefinition2)
	}
}
