package io.structify.domain.table.model

import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

internal class TableTest {

    @Test
    fun `should change current table version given new version of a table`() {
		// given
        val userId = UUID.randomUUID()
        val tableId = UUID.randomUUID()
		val table = Table(
			id = tableId,
			userId = userId,
			name = "People",
		)
		table.update(
			listOf(
                ColumnDefinition(
                    name = "name",
                    description = "Person name",
                    type = ColumnType.StringType(),
                    optional = false
                )
			)
        )

		// when
		table.update(
			listOf(
				ColumnDefinition(
					name = "name 2",
					description = "Person name 2",
					type = ColumnType.StringType(StringFormat.DATE),
					optional = false
				)
			)
		)

		// then
		val version = table.getCurrentVersion()
		assertThat(version.orderNumber).isEqualTo(2)
		assertThat(version.columns).containsExactlyInAnyOrder(
			ColumnDefinition(
				name = "name 2",
				description = "Person name 2",
				type = ColumnType.StringType(StringFormat.DATE),
				optional = false
			)
		)
    }
}
