package io.structify.infrastructure.table.persistence

import io.structify.domain.table.model.ColumnDefinition
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Table
import io.structify.domain.table.model.Version
import io.structify.infrastructure.test.DatabaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import java.util.UUID
import kotlin.test.Test

internal class ExposedTableRepositoryIntegrationTest : DatabaseIntegrationTest() {

    private val repo = ExposedTableRepository()

    @Test
    fun `should persist table aggregate into database`() = rollbackTransaction {
        // given
        val userId = UUID.randomUUID()
        val tableId = UUID.randomUUID()
        val v1Id = UUID.randomUUID()
        val v1 = Version(
            id = v1Id,
            description = "Initial version",
            columns = listOf(
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
                )
            ),
            orderNumber = 1
        )
        val table = Table(
            id = tableId,
            userId = userId,
            name = "People",
            version = v1
        )

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
        assertThat(loadedV1.id).isEqualTo(v1Id)
        assertThat(loadedV1.description).isEqualTo("Initial version")
        assertThat(loadedV1.orderNumber).isEqualTo(1)

        // columns
        assertThat(loadedV1.columns).hasSize(2)
        val nameCol = loadedV1.columns.first { it.name == "name" }
        assertThat(nameCol.description).isEqualTo("Person name")
        assertThat(nameCol.type).isEqualTo(ColumnType.StringType(format = StringFormat.DATE))
        assertThat(nameCol.optional).isFalse()

        val ageCol = loadedV1.columns.first { it.name == "age" }
        assertThat(ageCol.description).isEqualTo("Person age")
        assertThat(ageCol.type).isEqualTo(ColumnType.NumberType)
        assertThat(ageCol.optional).isTrue()
    }

    @Test
    fun `should upsert existing records when persisting same aggregate again`() = rollbackTransaction {
        // given
        val userId = UUID.randomUUID()
        val tableId = UUID.randomUUID()
        val v1Id = UUID.randomUUID()

        val v1 = Version(
            id = v1Id,
            description = "v1",
            columns = listOf(
                ColumnDefinition(
                    name = "name",
                    description = "name v1",
                    type = ColumnType.StringType(),
                    optional = false
                )
            ),
            orderNumber = 1
        )
        val initial = Table(
            id = tableId,
            userId = userId,
            name = "People",
            version = v1
        )
        repo.persist(initial)

        // when - update table name, update existing column, add new column, and add new version
        val v1Updated = Version(
            id = v1Id, // same id -> upsert
            description = "v1 updated",
            columns = listOf(
                ColumnDefinition(
                    name = "name", // same pk for columns (versionId+name)
                    description = "name v2",
                    type = ColumnType.StringType(format = StringFormat.DATE),
                    optional = true
                ),
                ColumnDefinition(
                    name = "age",
                    description = "age added",
                    type = ColumnType.NumberType,
                    optional = false
                )
            ),
            orderNumber = 1
        )
        val v2 = Version(
            id = UUID.randomUUID(),
            description = "v2",
            columns = listOf(
                ColumnDefinition(
                    name = "city",
                    description = "city of residence",
                    type = ColumnType.StringType(),
                    optional = true
                )
            ),
            orderNumber = 2
        )
        val updated = Table(
            id = tableId,
            userId = userId,
            name = "People Updated",
            version = v1Updated
        ).apply { add(v2) }

        repo.persist(updated)

        // then - load aggregate and assert via repository
        val found = repo.findById(userId, tableId)
        assertThat(found).isNotNull
        val loaded = found!!

        assertThat(loaded.name).isEqualTo("People Updated")

        // versions
        assertThat(loaded.versions).hasSize(2)
        val loadedV1 = loaded.versions.first { it.id == v1Id }
        assertThat(loadedV1.description).isEqualTo("v1 updated")

        // v1 columns upserted: name updated, age inserted
        assertThat(loadedV1.columns).hasSize(2)
        val nameCol = loadedV1.columns.first { it.name == "name" }
        assertThat(nameCol.description).isEqualTo("name v2")
        assertThat(nameCol.type).isEqualTo(ColumnType.StringType(format = StringFormat.DATE))
        assertThat(nameCol.optional).isTrue()
        val ageCol = loadedV1.columns.first { it.name == "age" }
        assertThat(ageCol.description).isEqualTo("age added")
        assertThat(ageCol.type).isEqualTo(ColumnType.NumberType)
        assertThat(ageCol.optional).isFalse()

        // v2 inserted with its column
        val loadedV2 = loaded.versions.first { it.id == v2.id }
        assertThat(loadedV2.columns).hasSize(1)
        assertThat(loadedV2.columns.first().name).isEqualTo("city")
    }
}
