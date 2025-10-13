package io.structify.infrastructure

import io.structify.infrastructure.security.NoAuthenticatedSubjectExceptionHandler
import io.structify.infrastructure.security.NoJwtExceptionHandler
import io.structify.infrastructure.security.installOAuthAuth
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun main(args: Array<String>) {
	io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
	installSerialization()
	install(StatusPages) {
		exception(NoJwtExceptionHandler)
		exception(NoAuthenticatedSubjectExceptionHandler)
	}
	val appComponent = DaggerStructifyAppComponent.builder()
		.applicationConfig(environment.config)
		.build()
	routing {
		val oAuthModule = installOAuthAuth()
		oAuthModule.securedRouting(this) {
			route("/api") {

			}
		}
	}
}
