package io.structify.domain.table.model

import java.util.UUID

data class Table(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val version: Version
) {

    var versions = setOf(version); private set

    fun add(version: Version)  {
        versions = versions + version
    }

    fun getCurrentVersion(): Version = versions.maxBy { it.orderNumber }
}
