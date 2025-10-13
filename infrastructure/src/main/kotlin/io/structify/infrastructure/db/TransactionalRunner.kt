package io.structify.infrastructure.db

import io.structify.domain.db.TransactionalRunner
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction

open class ExposedTransactionalRunner(
    private val database: Database
) : TransactionalRunner {
    override suspend fun <T> transaction(isolation: Int?, readOnly: Boolean, block: suspend () -> T): T {
        val current: Transaction? = TransactionManager.currentOrNull()
        return if (current != null) {
            current.withSuspendTransaction { block() }
        } else {
            suspendedTransactionAsync(
                Dispatchers.IO,
                db = database,
                transactionIsolation = isolation
            ) { block() }.await()
        }
    }
}
