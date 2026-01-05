package io.structify.infrastructure.table.readmodel

import io.structify.domain.table.model.ColumnDefinition
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Table
import io.structify.infrastructure.table.persistence.ExposedTableRepository
import io.structify.infrastructure.test.DatabaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

internal class VersionReadModelExposedRepositoryIntegrationTest : DatabaseIntegrationTest() {

	private val tableRepo = ExposedTableRepository()
	private val versionReadModelRepo = VersionReadModelExposedRepository()

	@Test
	fun `findAllVersionsByTableId should return all versions for table`() = rollbackTransaction {
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
					ColumnDefinition(
						name = "name",
						description = "Person name",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					),
				)
			)
			update(
				listOf(
					ColumnDefinition(
						name = "name",
						description = "Person name",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					),
					ColumnDefinition(
						name = "age",
						description = "Person age",
						type = ColumnType.NumberType,
						optional = true
					),
				)
			)
		}

		tableRepo.persist(table)

		// when
		val versions = versionReadModelRepo.findAllVersionsByTableId(userId, tableId)

		// then
		assertThat(versions).hasSize(2)

		val v1 = versions.first { it.orderNumber == 1 }
		assertThat(v1.columns).hasSize(1)
		assertThat(v1.columns[0].name).isEqualTo("name")
		assertThat(v1.columns[0].description).isEqualTo("Person name")
		assertThat(v1.columns[0].type.type).isEqualTo("STRING")
		assertThat(v1.columns[0].type.format).isEqualTo("DATE")
		assertThat(v1.columns[0].optional).isFalse()

		val v2 = versions.first { it.orderNumber == 2 }
		assertThat(v2.columns).hasSize(2)
		assertThat(v2.columns.map { it.name }).containsExactlyInAnyOrder("name", "age")
	}

	@Test
	fun `findCurrentVersionByTableId should return latest version for table`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()

		val table = Table(
			id = tableId,
			userId = userId,
			name = "Products",
		).apply {
			update(
				listOf(
					ColumnDefinition(
						name = "title",
						description = "Product title",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					),
				)
			)
			update(
				listOf(
					ColumnDefinition(
						name = "title",
						description = "Product title",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					),
					ColumnDefinition(
						name = "price",
						description = "Product price",
						type = ColumnType.NumberType,
						optional = false
					),
				)
			)
		}

		tableRepo.persist(table)

		// when
		val currentVersion = versionReadModelRepo.findCurrentVersionByTableId(userId, tableId)

		// then
		assertThat(currentVersion).isNotNull
		assertThat(currentVersion!!.orderNumber).isEqualTo(2)
		assertThat(currentVersion.columns).hasSize(2)
		assertThat(currentVersion.columns.map { it.name }).containsExactlyInAnyOrder("title", "price")
	}

	@Test
	fun `findCurrentVersionByTableIdOrThrow should return latest version for table`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()

		val table = Table(
			id = tableId,
			userId = userId,
			name = "Users",
		).apply {
			update(
				listOf(
					ColumnDefinition(
						name = "email",
						description = "User email",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					),
					ColumnDefinition(
						name = "active",
						description = "Is active",
						type = ColumnType.NumberType,
						optional = false
					),
				)
			)
		}

		tableRepo.persist(table)

		// when
		val currentVersion = versionReadModelRepo.findCurrentVersionByTableIdOrThrow(userId, tableId)

		// then
		assertThat(currentVersion.orderNumber).isEqualTo(1)
		assertThat(currentVersion.columns).hasSize(2)
		assertThat(currentVersion.columns).satisfiesExactlyInAnyOrder(
			{
				assertThat(it.name).isEqualTo("email")
				assertThat(it.type.type).isEqualTo("STRING")
				assertThat(it.type.format).isEqualTo("DATE")
			},
			{
				assertThat(it.name).isEqualTo("active")
				assertThat(it.type.type).isEqualTo("NUMBER")
				assertThat(it.type.format).isNull()
			}
		)
	}
}
