package io.structify.infrastructure.test

import io.structify.infrastructure.db.ExposedTransactionalRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

internal abstract class DatabaseIntegrationTest {
    protected val db = TestDatabaseConnectionProvider.provide()
    protected val keycloakDb = TestKeycloakDatabaseConnectionProvider.provide()

    protected val transactionalRunner = ExposedTransactionalRunner(db)

    protected fun <T> rollbackTransaction(test: suspend Transaction.() -> T): Unit = runBlocking {
        newSuspendedTransaction(Dispatchers.Default, db = db) {
            try {
                test()
            } catch (e: Throwable) {
                throw e
            } finally {
                rollback()
            }
        }
    }

    protected fun <T> rollbackKeycloakTransaction(test: suspend Transaction.() -> T): Unit = runBlocking {
        newSuspendedTransaction(Dispatchers.Default, db = keycloakDb) {
            try {
                test()
            } catch (e: Throwable) {
                throw e
            } finally {
                rollback()
            }
        }
    }
}
