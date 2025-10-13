package io.structify.infrastructure.test

import io.structify.infrastructure.security.SecuredRouting
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.mockk.every
import io.mockk.mockk
import java.util.*

const val MOCK_JWT_AUTH_PROVIDER_NAME = "mock-jwt-auth-provider"

internal class MockJwtAuthenticationProvider : AuthenticationProvider(Config()), SecuredRouting {

    var testPrincipal: JWTPrincipal? =
        mockk<JWTPrincipal>().apply { every { subject } returns UUID.randomUUID().toString() }

    fun setTestJwtPrincipalSubject(sub: String?) {
        testPrincipal = mockk<JWTPrincipal>().apply { every { subject } returns sub }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        testPrincipal?.let { context.principal(it) }
    }

    override fun invoke(routing: Routing, routes: Route.() -> Unit) {
        routing.authenticate(MOCK_JWT_AUTH_PROVIDER_NAME) { routes() }
    }

    private class Config() : AuthenticationProvider.Config(MOCK_JWT_AUTH_PROVIDER_NAME)
}

