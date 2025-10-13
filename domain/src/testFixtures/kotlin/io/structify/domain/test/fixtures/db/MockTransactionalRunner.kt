package io.structify.domain.test.fixtures.db

import io.structify.domain.db.TransactionalRunner

class MockTransactionalRunner : TransactionalRunner {
    override suspend fun <T> transaction(isolation: Int?, readOnly: Boolean, block: suspend () -> T): T {
        return block()
    }
}
