package io.structify.domain.table.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ColumnTest {

	@ParameterizedTest
	@MethodSource("sameColumns")
	fun `should consider columns equal`(
		column1: Column.Definition,
		column2: Column.Definition,
	) {
		val result = column1 == column2
		assertThat(result).isTrue
	}

	@ParameterizedTest
	@MethodSource("differentColumns")
	fun `should not consider columns equal`(
		column1: Column.Definition,
		column2: Column.Definition,
	) {
		val result = column1 == column2
		assertThat(result).isFalse
	}

	companion object {

		@JvmStatic
		fun sameColumns(): Stream<Arguments> {
			return listOf(
				Arguments.of(
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(StringFormat.DATE),
						optional = false
					),
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(StringFormat.DATE),
						optional = false
					)
				),
				Arguments.of(
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					),
					Column.Definition(
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
					Column.Definition(
						name = "name1",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					),
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					Column.Definition(
						name = "name",
						description = "description1",
						type = ColumnType.StringType(),
						optional = false
					),
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(StringFormat.DATE),
						optional = false
					),
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.NumberType,
						optional = false
					),
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.StringType(),
						optional = false
					)
				),
				Arguments.of(
					Column.Definition(
						name = "name",
						description = "description",
						type = ColumnType.NumberType,
						optional = false
					),
					Column.Definition(
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
