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

	@Test
	fun `should persist and load complex nested object columns correctly`() = rollbackTransaction {
		// given
		val userId = UUID.randomUUID()
		val tableId = UUID.randomUUID()

		val table = Table(
			id = tableId,
			userId = userId,
			name = "Company Data",
		).apply {
			update(
				listOf(
					// Root level string column
					Column.Definition(
						name = "companyName",
						description = "The name of the company",
						type = ColumnType.StringType(),
						optional = false
					),
					// Root level object column with nested object inside
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
								name = "address",
								description = "Detailed address information",
								type = ColumnType.ObjectType,
								optional = true,
								children = listOf(
									Column.Definition(
										name = "street",
										description = "Street name and number",
										type = ColumnType.StringType(),
										optional = false
									),
									Column.Definition(
										name = "zipCode",
										description = "Postal code",
										type = ColumnType.StringType(),
										optional = false
									)
								)
							)
						)
					)
				)
			)
		}

		// when
		repo.persist(table)

		// then
		val loaded = repo.findById(userId, tableId)
		assertThat(loaded).isNotNull()

		// Verify table basic info
		assertThat(loaded!!.id).isEqualTo(tableId)
		assertThat(loaded.userId).isEqualTo(userId)
		assertThat(loaded.name).isEqualTo("Company Data")

		// Verify version
		assertThat(loaded.versions).hasSize(1)
		val version = loaded.getCurrentVersion()
		assertThat(version.orderNumber).isEqualTo(1)

		// Verify root level columns
		assertThat(version.columns).hasSize(2)

		// Verify string column at root
		val companyNameCol = version.columns.first { it.definition.name == "companyName" }
		assertThat(companyNameCol.definition.description).isEqualTo("The name of the company")
		assertThat(companyNameCol.definition.type).isEqualTo(ColumnType.StringType())
		assertThat(companyNameCol.definition.optional).isFalse()
		assertThat(companyNameCol.children).isEmpty()

		// Verify object column at root
		val headquartersCol = version.columns.first { it.definition.name == "headquarters" }
		assertThat(headquartersCol.definition.description).isEqualTo("Company headquarters information")
		assertThat(headquartersCol.definition.type).isEqualTo(ColumnType.ObjectType)
		assertThat(headquartersCol.definition.optional).isFalse()
		assertThat(headquartersCol.children).hasSize(2)

		// Verify 1st level nested columns (children of headquarters)
		val cityCol = headquartersCol.children.first { it.definition.name == "city" }
		assertThat(cityCol.definition.description).isEqualTo("City where HQ is located")
		assertThat(cityCol.definition.type).isEqualTo(ColumnType.StringType())
		assertThat(cityCol.definition.optional).isFalse()
		assertThat(cityCol.children).isEmpty()

		val addressCol = headquartersCol.children.first { it.definition.name == "address" }
		assertThat(addressCol.definition.description).isEqualTo("Detailed address information")
		assertThat(addressCol.definition.type).isEqualTo(ColumnType.ObjectType)
		assertThat(addressCol.definition.optional).isTrue()
		assertThat(addressCol.children).hasSize(2)

		// Verify 2nd level nested columns (children of address)
		val streetCol = addressCol.children.first { it.definition.name == "street" }
		assertThat(streetCol.definition.description).isEqualTo("Street name and number")
		assertThat(streetCol.definition.type).isEqualTo(ColumnType.StringType())
		assertThat(streetCol.definition.optional).isFalse()
		assertThat(streetCol.children).isEmpty()

		val zipCodeCol = addressCol.children.first { it.definition.name == "zipCode" }
		assertThat(zipCodeCol.definition.description).isEqualTo("Postal code")
		assertThat(zipCodeCol.definition.type).isEqualTo(ColumnType.StringType())
		assertThat(zipCodeCol.definition.optional).isFalse()
		assertThat(zipCodeCol.children).isEmpty()

		// Verify column IDs are preserved (should be the same instances)
		assertThat(headquartersCol.id).isNotNull()
		assertThat(cityCol.id).isNotNull()
		assertThat(addressCol.id).isNotNull()
		assertThat(streetCol.id).isNotNull()
		assertThat(zipCodeCol.id).isNotNull()

		// Verify all IDs are different
		val allColumnIds = listOf(
			companyNameCol.id,
			headquartersCol.id,
			cityCol.id,
			addressCol.id,
			streetCol.id,
			zipCodeCol.id
		)
		assertThat(allColumnIds).doesNotHaveDuplicates()
	}
}
