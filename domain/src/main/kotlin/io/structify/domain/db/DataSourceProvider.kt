package io.structify.domain.db

import javax.sql.DataSource

fun interface DataSourceProvider {
    @Throws(IllegalStateException::class)
    fun provide(): DataSource
}
