package io.structify.infrastructure.table.readmodel

import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Version
import io.structify.infrastructure.test.DatabaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import java.util.*
import kotlin.test.Test

internal class VersionReadModelExposedRepositoryIntegrationTest : DatabaseIntegrationTest() {

	private val tableReadModelRepo = ExposedTableReadModelRepository()
	private val versionReadModelRepo = VersionReadModelExposedRepository()

	@Test
	fun `findAllVersionsByTableId should return all versions for table`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()

		// Set up read model: table entry required for join
		tableReadModelRepo.create(tableId, userId, "People", "")

		val version1 = Version(
			columns = listOf(
				Column(
					definition = Column.Definition(
						name = "name",
						description = "Person name",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					)
				)
			),
			orderNumber = 1
		)
		val version2 = Version(
			columns = listOf(
				Column(
					definition = Column.Definition(
						name = "name",
						description = "Person name",
						type = ColumnType.StringType(format = StringFormat.DATE),
						optional = false
					)
				),
				Column(
					definition = Column.Definition(
						name = "age",
						description = "Person age",
						type = ColumnType.NumberType,
						optional = true
					)
				)
			),
			orderNumber = 2
		)

		versionReadModelRepo.upsertVersion(tableId, userId, version1)
		versionReadModelRepo.upsertVersion(tableId, userId, version2)

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

		tableReadModelRepo.create(tableId, userId, "Products", "")

		versionReadModelRepo.upsertVersion(
			tableId, userId,
			Version(
				columns = listOf(
					Column(
						definition = Column.Definition(
							name = "title",
							description = "Product title",
							type = ColumnType.StringType(format = StringFormat.DATE),
							optional = false
						)
					)
				),
				orderNumber = 1
			)
		)
		versionReadModelRepo.upsertVersion(
			tableId, userId,
			Version(
				columns = listOf(
					Column(
						definition = Column.Definition(
							name = "title",
							description = "Product title",
							type = ColumnType.StringType(format = StringFormat.DATE),
							optional = false
						)
					),
					Column(
						definition = Column.Definition(
							name = "price",
							description = "Product price",
							type = ColumnType.NumberType,
							optional = false
						)
					)
				),
				orderNumber = 2
			)
		)

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

		tableReadModelRepo.create(tableId, userId, "Users", "")

		versionReadModelRepo.upsertVersion(
			tableId, userId,
			Version(
				columns = listOf(
					Column(
						definition = Column.Definition(
							name = "email",
							description = "User email",
							type = ColumnType.StringType(format = StringFormat.DATE),
							optional = false
						)
					),
					Column(
						definition = Column.Definition(
							name = "active",
							description = "Is active",
							type = ColumnType.NumberType,
							optional = false
						)
					)
				),
				orderNumber = 1
			)
		)

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
