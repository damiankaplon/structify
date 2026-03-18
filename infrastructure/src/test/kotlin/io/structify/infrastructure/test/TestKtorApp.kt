package io.structify.infrastructure.test

import dagger.Component
import dagger.Module
import dagger.Provides
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.table.TableCommandHandler
import io.structify.domain.test.fixtures.db.MockTransactionalRunner
import io.structify.domain.test.fixtures.row.RowInMemoryRepository
import io.structify.domain.test.fixtures.table.TableInMemoryRepository
import io.structify.infrastructure.installApp
import io.structify.infrastructure.row.extractors.MockRowExtractor
import io.structify.infrastructure.row.readmodel.RowReadModelInMemoryRepository
import io.structify.infrastructure.table.event.TableCreatedDomainEventHandler
import io.structify.infrastructure.table.event.TableVersionCreatedReadModelEventHandler
import io.structify.infrastructure.table.readmodel.TableReadModelInMemoryRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelInMemoryRepository
import jakarta.inject.Singleton

@Module
internal class TestAppModule {

	@Provides
	@Singleton
	fun provideTableRepository(): TableInMemoryRepository = TableInMemoryRepository()

	@Provides
	@Singleton
	fun provideTransactionalRunner(): TransactionalRunner = MockTransactionalRunner()

	@Provides
	@Singleton
	fun provideMockJwtAuthenticationProvider(): MockJwtAuthenticationProvider {
		return MockJwtAuthenticationProvider()
	}

	@Provides
	@Singleton
	fun provideVersionReadModelRepository(): VersionReadModelInMemoryRepository =
		VersionReadModelInMemoryRepository()

	@Provides
	@Singleton
	fun provideTableReadModelRepository(): TableReadModelInMemoryRepository =
		TableReadModelInMemoryRepository()

	@Provides
	@Singleton
	fun provideRowRepository(): RowInMemoryRepository = RowInMemoryRepository()

	@Provides
	@Singleton
	fun provideRowReadModelRepository(): RowReadModelInMemoryRepository = RowReadModelInMemoryRepository()

	@Provides
	@Singleton
	fun provideRowExtractor(): MockRowExtractor = MockRowExtractor()

	@Provides
	@Singleton
	fun provideTableCommandHandler(tableRepository: TableInMemoryRepository): TableCommandHandler =
		TableCommandHandler(tableRepository)

	@Provides
	@Singleton
	fun provideTableCreatedDomainEventHandler(tableRepository: TableInMemoryRepository): TableCreatedDomainEventHandler =
		TableCreatedDomainEventHandler(tableRepository)

	@Provides
	@Singleton
	fun provideTableVersionCreatedReadModelEventHandler(
		versionReadModelRepository: VersionReadModelInMemoryRepository,
	): TableVersionCreatedReadModelEventHandler =
		TableVersionCreatedReadModelEventHandler(versionReadModelRepository)
}

@Singleton
@Component(modules = [TestAppModule::class])
internal interface TestAppComponent {

	fun tableRepository(): TableInMemoryRepository
	fun transactionalRunner(): TransactionalRunner
	fun mockJwtAuthenticationProvider(): MockJwtAuthenticationProvider
	fun versionReadModelRepository(): VersionReadModelInMemoryRepository
	fun tableReadModelRepository(): TableReadModelInMemoryRepository
	fun rowRepository(): RowInMemoryRepository
	fun rowReadModelRepository(): RowReadModelInMemoryRepository
	fun mockRowExtractor(): MockRowExtractor
	fun tableCommandHandler(): TableCommandHandler
	fun tableCreatedDomainEventHandler(): TableCreatedDomainEventHandler
	fun tableVersionCreatedReadModelEventHandler(): TableVersionCreatedReadModelEventHandler
}

internal fun ApplicationTestBuilder.setupTestApp(): TestAppComponent {
	val testAppComponent = DaggerTestAppComponent.create()
	application {
		install(Authentication) { register(testAppComponent.mockJwtAuthenticationProvider()) }
		installApp(
			testAppComponent.mockJwtAuthenticationProvider(),
			testAppComponent.transactionalRunner(),
			testAppComponent.tableCommandHandler(),
			testAppComponent.tableRepository(),
			testAppComponent.versionReadModelRepository(),
			testAppComponent.tableReadModelRepository(),
			testAppComponent.tableCreatedDomainEventHandler(),
			testAppComponent.tableVersionCreatedReadModelEventHandler(),
			testAppComponent.rowRepository(),
			testAppComponent.rowReadModelRepository(),
			testAppComponent.mockRowExtractor()
		)
	}
	return testAppComponent
}
