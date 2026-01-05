package io.structify.infrastructure

import dagger.BindsInstance
import dagger.Component
import io.ktor.server.config.ApplicationConfig
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.row.RowRepository
import io.structify.domain.table.TableRepository
import io.structify.infrastructure.db.dagger.DatabaseModule
import io.structify.infrastructure.row.dagger.RowRepositoryModule
import io.structify.infrastructure.row.readmodel.RowReadModelRepository
import io.structify.infrastructure.table.dagger.TableRepositoryModule
import io.structify.infrastructure.table.readmodel.TableReadModelRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository
import jakarta.inject.Singleton
import org.jetbrains.exposed.sql.Database

@Singleton
@Component(
    modules = [
        DatabaseModule::class,
        TableRepositoryModule::class,
		RowRepositoryModule::class,
    ]
)
interface StructifyAppComponent {
    fun database(): Database
    fun transactionalRunner(): TransactionalRunner
    fun tableRepository(): TableRepository
	fun versionReadModelRepository(): VersionReadModelRepository
	fun tableReadModelRepository(): TableReadModelRepository
	fun rowRepository(): RowRepository
	fun rowReadModelRepository(): RowReadModelRepository

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun applicationConfig(config: ApplicationConfig): Builder
        fun build(): StructifyAppComponent
    }
}
