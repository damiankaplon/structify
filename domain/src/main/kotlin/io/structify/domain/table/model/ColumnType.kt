package io.structify.domain.table.model

sealed class ColumnType {
    data class StringType(val format: StringFormat? = null) : ColumnType()
    object NumberType : ColumnType()
	object ObjectType : ColumnType()
}

enum class StringFormat {
    DATE
}
