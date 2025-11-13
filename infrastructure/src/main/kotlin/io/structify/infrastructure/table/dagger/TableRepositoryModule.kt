package io.structify.infrastructure.table.dagger

import dagger.Module
import dagger.Provides
import io.structify.domain.table.TableRepository
import io.structify.infrastructure.table.persistence.ExposedTableRepository
import io.structify.infrastructure.table.readmodel.ExposedVersionReadModelRepository
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
		return ExposedVersionReadModelRepository()
	}
}
