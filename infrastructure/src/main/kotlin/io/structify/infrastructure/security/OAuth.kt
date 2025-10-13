package io.structify.infrastructure.security

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.net.URI

private const val JWT_CONFIG = "jwt-config"


class JwtPropertiesConfigFileProvider(private val env: ApplicationEnvironment) {
    val jwkUrl get() = env.config.property("jwt.jwk-url").getString()
    val issuer get() = env.config.property("jwt.issuer").getString()
}

fun Application.installOAuthAuth(): OAuthAuthModule {
    install(Authentication) {
        jwt(JWT_CONFIG) {
            val properties = JwtPropertiesConfigFileProvider(this@installOAuthAuth.environment)
            val jwkEndpointUrl =
                URI.create(properties.jwkUrl).toURL()
            val jwkProvider = JwkProviderBuilder(jwkEndpointUrl).build()
            verifier(jwkProvider, properties.issuer) { /* No additional constraints */ }
            validate { jwtCredential -> JWTPrincipal(jwtCredential.payload) }
            challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized) }
        }

    }
    return OAuthAuthModule { routing, routes -> routing.authenticate(JWT_CONFIG) { routes() } }
}

class OAuthAuthModule(
    val securedRouting: SecuredRouting
)
