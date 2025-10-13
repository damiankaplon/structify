package io.structify.infrastructure.db

import io.structify.domain.db.DatabaseConfig
import io.structify.domain.db.DatabaseConfigProvider
import io.ktor.server.config.*

class KtorEnvDatabaseConfigProvider(
    private val ktorAppConfig: ApplicationConfig
) : DatabaseConfigProvider {
    override fun provide(): DatabaseConfig =
        DatabaseConfig(
            jdbcUrl = ktorAppConfig.property("db.jdbcUrl").getString(),
            user = ktorAppConfig.property("db.user").getString(),
            password = ktorAppConfig.property("db.password").getString(),
            schema = ktorAppConfig.property("db.schema").getString(),
        )
}
