package io.structify.domain.table.model

data class ColumnDefinition(
    val name: String,
    val description: String,
    val type: ColumnType,
    val optional: Boolean
)
