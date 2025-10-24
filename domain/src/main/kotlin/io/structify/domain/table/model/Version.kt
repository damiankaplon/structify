package io.structify.domain.table.model

import java.util.UUID

data class Version(
    val id: UUID,
    val description: String,
    val columns: List<ColumnDefinition>,
    val orderNumber: Int
)
