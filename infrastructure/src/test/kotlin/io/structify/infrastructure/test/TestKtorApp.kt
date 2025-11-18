package io.structify.infrastructure.test

import dagger.Component
import dagger.Module
import dagger.Provides
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.testing.ApplicationTestBuilder
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.test.fixtures.db.MockTransactionalRunner
import io.structify.domain.test.fixtures.table.TableInMemoryRepository
import io.structify.infrastructure.installApp
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
}

@Singleton
@Component(modules = [TestAppModule::class])
internal interface TestAppComponent {

	fun tableRepository(): TableInMemoryRepository
	fun transactionalRunner(): TransactionalRunner
	fun mockJwtAuthenticationProvider(): MockJwtAuthenticationProvider
	fun versionReadModelRepository(): VersionReadModelInMemoryRepository
	fun tableReadModelRepository(): TableReadModelInMemoryRepository
}

internal fun ApplicationTestBuilder.setupTestApp(): TestAppComponent {
	val testAppComponent = DaggerTestAppComponent.create()
	application {
		install(Authentication) { register(testAppComponent.mockJwtAuthenticationProvider()) }
		installApp(
			testAppComponent.mockJwtAuthenticationProvider(),
			testAppComponent.transactionalRunner(),
			testAppComponent.tableRepository(),
			testAppComponent.versionReadModelRepository(),
			testAppComponent.tableReadModelRepository(),
		)
	}
	return testAppComponent
}