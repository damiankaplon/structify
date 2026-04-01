package io.structify.infrastructure

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.row.RowExtractor
import io.structify.domain.row.RowRepository
import io.structify.domain.table.TableCommandHandler
import io.structify.domain.table.TableRepository
import io.structify.infrastructure.db.NoEntityFoundExceptionHandler
import io.structify.infrastructure.row.api.rowRoutes
import io.structify.infrastructure.row.readmodel.RowReadModelRepository
import io.structify.infrastructure.security.NoAuthenticatedSubjectExceptionHandler
import io.structify.infrastructure.security.NoJwtExceptionHandler
import io.structify.infrastructure.security.SecuredRouting
import io.structify.infrastructure.security.installOAuthAuth
import io.structify.infrastructure.table.api.tableRoutes
import io.structify.infrastructure.table.event.TableCreatedDomainEventHandler
import io.structify.infrastructure.table.event.TableVersionCreatedReadModelEventHandler
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
		appComponent.tableCommandHandler(),
		appComponent.tableRepository(),
		appComponent.versionReadModelRepository(),
		appComponent.tableReadModelRepository(),
		appComponent.tableCreatedDomainEventHandler(),
		appComponent.tableVersionCreatedReadModelEventHandler(),
		appComponent.rowRepository(),
		appComponent.rowReadModelRepository(),
		appComponent.rowExtractor(),
	)
	val hostedReactAppPath = environment.config.propertyOrNull("app.ui.path")?.getString()
	if (hostedReactAppPath != null) {
		routing {
			singlePageApplication {
				react(hostedReactAppPath)
			}
		}
	}
}

fun Application.installApp(
	securedRouting: SecuredRouting,
	transactionalRunner: TransactionalRunner,
	tableCommandHandler: TableCommandHandler,
	tableRepository: TableRepository,
	versionReadModelRepository: VersionReadModelRepository,
	tableReadModelRepository: TableReadModelRepository,
	tableCreatedDomainEventHandler: TableCreatedDomainEventHandler,
	tableVersionCreatedReadModelEventHandler: TableVersionCreatedReadModelEventHandler,
	rowRepository: RowRepository,
	rowReadModelRepository: RowReadModelRepository,
	rowExtractor: RowExtractor,
) {
	installSerialization()
	install(StatusPages) {
		exception(NoJwtExceptionHandler)
		exception(NoAuthenticatedSubjectExceptionHandler)
		exception<NoSuchElementException> { call, cause ->
			call.respondText(cause.message ?: "Resource not found", status = HttpStatusCode.NotFound)
		}
		exception<IllegalArgumentException> { call, cause ->
			call.respondText(cause.message ?: "Bad request", status = HttpStatusCode.BadRequest)
		}
		exception(NoEntityFoundExceptionHandler)
	}
	installRouting(
		securedRouting,
		transactionalRunner,
		tableCommandHandler,
		tableRepository,
		versionReadModelRepository,
		tableReadModelRepository,
		tableCreatedDomainEventHandler,
		tableVersionCreatedReadModelEventHandler,
		rowRepository,
		rowReadModelRepository,
		rowExtractor,
	)
}

fun Application.installRouting(
	securedRouting: SecuredRouting,
	transactionalRunner: TransactionalRunner,
	tableCommandHandler: TableCommandHandler,
	tableRepository: TableRepository,
	versionReadModelRepository: VersionReadModelRepository,
	tableReadModelRepository: TableReadModelRepository,
	tableCreatedDomainEventHandler: TableCreatedDomainEventHandler,
	tableVersionCreatedReadModelEventHandler: TableVersionCreatedReadModelEventHandler,
	rowRepository: RowRepository,
	rowReadModelRepository: RowReadModelRepository,
	rowExtractor: RowExtractor,
) {
	routing {
		securedRouting.invoke(this) {
			route("/api") {
				tableRoutes(
					transactionalRunner,
					tableCommandHandler,
					versionReadModelRepository,
					tableReadModelRepository,
					tableCreatedDomainEventHandler,
					tableVersionCreatedReadModelEventHandler,
				)
				rowRoutes(
					transactionalRunner,
					rowRepository,
					rowReadModelRepository,
					tableRepository,
					rowExtractor,
				)
			}
		}
	}
}
