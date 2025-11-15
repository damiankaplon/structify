package io.structify.infrastructure.db

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.util.logging.KtorSimpleLogger

private val LOGGER = KtorSimpleLogger(NoEntityFoundExceptionHandler::class.java.name)

class NoEntityFoundException(message: String) : RuntimeException(message)

object NoEntityFoundExceptionHandler : suspend (ApplicationCall, NoEntityFoundException) -> Unit {

	override suspend fun invoke(call: ApplicationCall, ex: NoEntityFoundException) {
		LOGGER.error(ex.message)
		call.respond(HttpStatusCode.NotFound, null)
	}
}
