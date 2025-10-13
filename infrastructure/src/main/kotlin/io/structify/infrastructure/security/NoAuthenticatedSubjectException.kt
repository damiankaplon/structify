package io.structify.infrastructure.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.logging.*

private val LOGGER = KtorSimpleLogger(NoAuthenticatedSubjectExceptionHandler::class.java.name)

class NoAuthenticatedSubjectException(message: String) : RuntimeException(message)

object NoAuthenticatedSubjectExceptionHandler : suspend (ApplicationCall, NoAuthenticatedSubjectException) -> Unit {

    override suspend fun invoke(
        call: ApplicationCall,
        ex: NoAuthenticatedSubjectException
    ) {
        LOGGER.error(ex.message)
        call.respond(HttpStatusCode.Unauthorized, null)
    }
}
