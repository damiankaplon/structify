package io.structify.infrastructure

import dagger.BindsInstance
import dagger.Component
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.table.TableRepository
import io.structify.infrastructure.db.dagger.DatabaseModule
import io.structify.infrastructure.table.dagger.TableRepositoryModule
import io.ktor.server.config.*
import jakarta.inject.Singleton
import org.jetbrains.exposed.sql.Database

@Singleton
@Component(
    modules = [
        DatabaseModule::class,
        TableRepositoryModule::class,
    ]
)
interface StructifyAppComponent {
    fun database(): Database
    fun transactionalRunner(): TransactionalRunner
    fun tableRepository(): TableRepository

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun applicationConfig(config: ApplicationConfig): Builder
        fun build(): StructifyAppComponent
    }
}
