package io.structify.infrastructure.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.logging.*

private val LOGGER = KtorSimpleLogger(NoJwtExceptionHandler::class.java.name)

class NoJwtException(message: String) : RuntimeException(message)

object NoJwtExceptionHandler : suspend (ApplicationCall, NoJwtException) -> Unit {

    override suspend fun invoke(call: ApplicationCall, ex: NoJwtException) {
        LOGGER.error(ex.stackTraceToString(), ex)
        call.respond(HttpStatusCode.Unauthorized, null)
    }
}
