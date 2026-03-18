package io.structify.infrastructure.table.dagger

import dagger.Module
import dagger.Provides
import io.structify.domain.table.TableCommandHandler
import io.structify.domain.table.TableRepository
import io.structify.infrastructure.table.event.TableCreatedDomainEventHandler
import io.structify.infrastructure.table.event.TableVersionCreatedReadModelEventHandler
import io.structify.infrastructure.table.persistence.ExposedTableRepository
import io.structify.infrastructure.table.readmodel.ExposedTableReadModelRepository
import io.structify.infrastructure.table.readmodel.TableReadModelRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelExposedRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository
import jakarta.inject.Singleton

@Module
class TableRepositoryModule {

	@Provides
	@Singleton
	fun provideTableRepository(): TableRepository {
		return ExposedTableRepository()
	}

	@Provides
	@Singleton
	fun provideVersionReadModelRepository(): VersionReadModelRepository {
		return VersionReadModelExposedRepository()
	}

	@Provides
	@Singleton
	fun provideTableReadModelRepository(): TableReadModelRepository {
		return ExposedTableReadModelRepository()
	}

	@Provides
	@Singleton
	fun provideTableCommandHandler(tableRepository: TableRepository): TableCommandHandler {
		return TableCommandHandler(tableRepository)
	}

	@Provides
	@Singleton
	fun provideTableCreatedDomainEventHandler(tableRepository: TableRepository): TableCreatedDomainEventHandler {
		return TableCreatedDomainEventHandler(tableRepository)
	}

	@Provides
	@Singleton
	fun provideTableVersionCreatedReadModelEventHandler(
		versionReadModelRepository: VersionReadModelRepository,
	): TableVersionCreatedReadModelEventHandler {
		return TableVersionCreatedReadModelEventHandler(versionReadModelRepository)
	}
}
