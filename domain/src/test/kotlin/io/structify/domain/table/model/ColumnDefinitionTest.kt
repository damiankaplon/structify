package io.structify.domain.table.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

internal class ColumnDefinitionTest {

	@ParameterizedTest
	@MethodSource("sameColumns")
	fun `should consider columns equal`(
		column1: ColumnDefinition,
		column2: ColumnDefinition,
	) {
		val result = column1.sameDefinitionAs(column2)
		assertThat(result).isTrue
	}

	@ParameterizedTest
	@MethodSource("differentColumns")
	fun `should not consider columns equal`(
		column1: ColumnDefinition,
		column2: ColumnDefinition,
	) {
		val result = column1.sameDefinitionAs(column2)
		assertThat(result).isFalse
	}

	companion object {

		@JvmStatic
		fun sameColumns(): Stream<Arguments> {
			return listOf(
				Arguments.of(
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(StringFormat.DATE),
						optional = false
					),
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(StringFormat.DATE),
						optional = false
					)
				),
				Arguments.of(
					ColumnDefinition(
						id = UUID.randomUUID(),
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					),
					ColumnDefinition(
						id = UUID.randomUUID(),
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
			).stream()
		}

		@JvmStatic
		fun differentColumns(): Stream<Arguments> {
			return listOf(
				Arguments.of(
					ColumnDefinition(
						name = "name1",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					),
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					ColumnDefinition(
						name = "name",
						description = "description1",
						type = ColumnType.StringType(),
						optional = false
					),
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(StringFormat.DATE),
						optional = false
					),
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.NumberType,
						optional = false
					),
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.NumberType,
						optional = false
					),
					ColumnDefinition(
						name = "name",
						description = "description",
						type = ColumnType.NumberType,
						optional = true
					)
				),
			).stream()
		}
	}
}
