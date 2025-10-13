package io.structify.infrastructure.test.db

import io.structify.infrastructure.db.ExposedTransactionalRunner
import io.structify.infrastructure.test.DatabaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import kotlin.test.Test

internal class ExposedTransactionalRunnerIntegrationTest : DatabaseIntegrationTest() {

    private val testee = ExposedTransactionalRunner(super.db)

    @Test
    fun `should rollback outer outer transaction when inner is rolled back`() = rollbackTransaction {
        // given
        SchemaUtils.create(ExposedTransactionalRunnerTestDao.ExposedTransactionalRunnerTestTable)

        // when
        testee.transaction {
            ExposedTransactionalRunnerTestDao.new {
                property = "outer-transaction"
            }
            testee.transaction {
                ExposedTransactionalRunnerTestDao.new {
                    property = "inner-transaction"
                }
                flushCache()
                rollback()
            }
        }

        // then
        val result = ExposedTransactionalRunnerTestDao.all()
        assertThat(result).isEmpty()
    }

    @Test
    fun `should rollback inner transaction when outer is rolled back`() = rollbackTransaction {
        // given
        SchemaUtils.create(ExposedTransactionalRunnerTestDao.ExposedTransactionalRunnerTestTable)

        // when
        testee.transaction {
            ExposedTransactionalRunnerTestDao.new {
                property = "outer-transaction"
            }
            testee.transaction {
                ExposedTransactionalRunnerTestDao.new {
                    property = "inner-transaction"
                }
            }
            flushCache()
            rollback()
        }

        // then
        val result = ExposedTransactionalRunnerTestDao.all()
        assertThat(result).isEmpty()
    }

    class ExposedTransactionalRunnerTestDao(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<ExposedTransactionalRunnerTestDao>(ExposedTransactionalRunnerTestTable)

        var property: String by ExposedTransactionalRunnerTestTable.property

        object ExposedTransactionalRunnerTestTable :
            LongIdTable("exposed_transactional_runner_integration_test_table") {
            var property = text("property")
        }
    }
}