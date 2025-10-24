package io.structify.domain.table.model

import java.util.UUID

data class Table(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val version: Version
) {

    private var versions = setOf(version)

    fun add(version: Version)  {
        versions = versions + version
    }

    fun getCurrentVersion(): Version = versions.maxBy { it.orderNumber }
}
