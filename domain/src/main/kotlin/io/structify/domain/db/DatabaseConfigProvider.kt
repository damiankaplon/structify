package io.structify.domain.db

data class DatabaseConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
    val schema: String = "public",
)

fun interface DatabaseConfigProvider {
    @Throws(IllegalStateException::class)
    fun provide(): DatabaseConfig
}