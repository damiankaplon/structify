package io.structify.infrastructure.db.dagger

import dagger.Module
import dagger.Provides
import io.structify.domain.db.TransactionalRunner
import io.structify.infrastructure.db.ExposedTransactionalRunner
import io.structify.infrastructure.db.HikariCPDataSourceProvider
import io.structify.infrastructure.db.KtorEnvDatabaseConfigProvider
import io.ktor.server.config.*
import jakarta.inject.Singleton
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideDataSource(ktorAppConfig: ApplicationConfig): DataSource {
        val databaseConfigProvider = KtorEnvDatabaseConfigProvider(ktorAppConfig)
        return HikariCPDataSourceProvider(databaseConfigProvider, ktorAppConfig).provide()
    }

    @Provides
    @Singleton
    fun provideExposedDatabase(dataSource: DataSource): Database {
        val database = Database.connect(dataSource)
        return database
    }

    @Provides
    @Singleton
    fun provideTransactionRunner(database: Database): TransactionalRunner {
        return ExposedTransactionalRunner(database)
    }
}
