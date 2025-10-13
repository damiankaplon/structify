package io.structify.infrastructure.test

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

val ApplicationTestBuilder.clientJson: HttpClient
    get() = createClient { install(ContentNegotiation) { json() } }
