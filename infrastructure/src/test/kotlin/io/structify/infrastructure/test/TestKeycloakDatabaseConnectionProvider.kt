package io.structify.infrastructure.test

import org.jetbrains.exposed.sql.Database

internal object TestKeycloakDatabaseConnectionProvider {

    fun provide(): Database {
        return Database.connect(
            url = "jdbc:postgresql://localhost:5432/keycloak?serverTimezone=UTC",
            user = "keycloak",
            password = "keycloak"
        )
    }
}
