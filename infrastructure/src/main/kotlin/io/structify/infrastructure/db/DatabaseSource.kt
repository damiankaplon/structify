package io.structify.infrastructure.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.structify.domain.db.DataSourceProvider
import io.structify.domain.db.DatabaseConfigProvider
import io.ktor.server.config.*
import javax.sql.DataSource

class HikariCPDataSourceProvider(
    private val databaseConfigProvider: DatabaseConfigProvider,
    private val ktorAppConfig: ApplicationConfig
) : DataSourceProvider {
    override fun provide(): DataSource {
        val dbConfig = databaseConfigProvider.provide()
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.jdbcUrl
            username = dbConfig.user
            password = dbConfig.password
            schema = dbConfig.schema
            maximumPoolSize = ktorAppConfig.propertyOrNull("db.poolSize")?.getString()?.toInt() ?: 10
        }
        return HikariDataSource(hikariConfig)
    }
}
