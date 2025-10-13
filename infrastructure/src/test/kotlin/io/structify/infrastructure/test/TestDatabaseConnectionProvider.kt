package io.structify.infrastructure.test

import org.jetbrains.exposed.sql.Database

internal object TestDatabaseConnectionProvider {

    fun provide(): Database {
        return Database.connect(
            url = "jdbc:postgresql://localhost:5432/structify?serverTimezone=UTC",
            user = "structify",
            password = "structify"
        )
    }
}
