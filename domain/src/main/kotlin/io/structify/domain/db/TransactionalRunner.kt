package io.structify.domain.db

interface TransactionalRunner {

    suspend fun <T> transaction(
        isolation: Int? = null,
        readOnly: Boolean = false,
        block: suspend () -> T
    ): T
}
