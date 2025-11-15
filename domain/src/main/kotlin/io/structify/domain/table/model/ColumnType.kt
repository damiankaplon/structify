package io.structify.domain.table.model

sealed class ColumnType {
    data class StringType(val format: StringFormat? = null) : ColumnType()
    object NumberType : ColumnType()
}

enum class StringFormat {
    DATE
}
