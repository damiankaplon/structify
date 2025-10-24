package io.structify.domain.table.model

import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

internal class TableTest {

    @Test
    fun `should change current table version given new version of a table`() {
        // Given an initial table with version 1
        val userId = UUID.randomUUID()
        val tableId = UUID.randomUUID()
        val v1 = Version(
            id = UUID.randomUUID(),
            description = "Initial version",
            columns = listOf(
                ColumnDefinition(
                    name = "name",
                    description = "Person name",
                    type = ColumnType.StringType(),
                    optional = false
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

        assertThat(table.getCurrentVersion()).isEqualTo(v1)

        val v2 = Version(
            id = UUID.randomUUID(),
            description = "Second version",
            columns = v1.columns,
            orderNumber = 2
        )
        table.add(v2)

        assertThat(table.getCurrentVersion()).isEqualTo(v2)
    }
}
