package io.structify.infrastructure.security

import io.structify.java.toUUID
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*

fun RoutingCall.jwtPrincipalOrThrow(): Principal = this.principal<JWTPrincipal>()
    ?.let {
        object : Principal {
            override val userId = it.subject.toUUID()
        }
    } ?: throw NoJwtException("No JWT principal found")
