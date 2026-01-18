package io.structify.infrastructure.row.extractos.openai

import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol.Companion.HTTPS
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import jakarta.inject.Singleton

interface ChatGptHttpClient {

	val client: HttpClient
}

@Module
class ChatGptHttpClientModule {

	@Provides
	@Singleton
	fun provideHttpClient(config: ApplicationConfig): ChatGptHttpClient = object : ChatGptHttpClient {
		override val client: HttpClient = HttpClient(CIO) {
			defaultRequest {
				url { protocol = HTTPS }
				host = config.property("app.chatGpt.host").getString()
				headers {
					bearerAuth(config.property("app.chatGpt.apiKey").getString())
				}
			}

			install(HttpTimeout) { requestTimeoutMillis = 120000 }
			install(ContentNegotiation) { json() }
			install(Logging) {
				level = LogLevel.ALL
				sanitizeHeader { header -> header == HttpHeaders.Authorization }
			}
		}
	}
}