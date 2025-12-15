package io.structify.infrastructure

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.table.TableRepository
import io.structify.infrastructure.security.NoAuthenticatedSubjectExceptionHandler
import io.structify.infrastructure.security.NoJwtExceptionHandler
import io.structify.infrastructure.security.SecuredRouting
import io.structify.infrastructure.security.installOAuthAuth
import io.structify.infrastructure.table.api.tableRoutes
import io.structify.infrastructure.table.readmodel.TableReadModelRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository

fun main(args: Array<String>) {
	io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
	val appComponent = DaggerStructifyAppComponent.builder()
		.applicationConfig(environment.config)
		.build()
	val oauthModule = installOAuthAuth()
	installApp(
		oauthModule.securedRouting,
		appComponent.transactionalRunner(),
		appComponent.tableRepository(),
		appComponent.versionReadModelRepository(),
		appComponent.tableReadModelRepository(),
	)
}

fun Application.installApp(
	securedRouting: SecuredRouting,
	transactionalRunner: TransactionalRunner,
	tableRepository: TableRepository,
	versionReadModelRepository: VersionReadModelRepository,
	tableReadModelRepository: TableReadModelRepository,
) {
	installSerialization()
	install(StatusPages) {
		exception(NoJwtExceptionHandler)
		exception(NoAuthenticatedSubjectExceptionHandler)
	}
	installRouting(securedRouting, transactionalRunner, tableRepository, versionReadModelRepository, tableReadModelRepository)
}

fun Application.installRouting(
	securedRouting: SecuredRouting,
	transactionalRunner: TransactionalRunner,
	tableRepository: TableRepository,
	versionReadModelRepository: VersionReadModelRepository,
	tableReadModelRepository: TableReadModelRepository,
) {
	routing {
		securedRouting.invoke(this) {
			route("/api") {
				tableRoutes(
					transactionalRunner,
					tableRepository,
					versionReadModelRepository,
					tableReadModelRepository,
				)
			}
		}
	}
}
