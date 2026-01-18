package io.structify.infrastructure.row.dagger

import dagger.Module
import dagger.Provides
import io.structify.domain.row.RowExtractor
import io.structify.domain.row.RowRepository
import io.structify.infrastructure.kotlinx.serialization.json
import io.structify.infrastructure.row.extractos.openai.ChatGptHttpClient
import io.structify.infrastructure.row.extractos.openai.OpenAiExtractor
import io.structify.infrastructure.row.persistence.ExposedRowRepository
import io.structify.infrastructure.row.readmodel.ExposedRowReadModelRepository
import io.structify.infrastructure.row.readmodel.RowReadModelRepository
import jakarta.inject.Singleton

@Module
class RowModule {

	@Provides
	@Singleton
	fun provideRowRepository(): RowRepository {
		return ExposedRowRepository()
	}

	@Provides
	@Singleton
	fun provideRowReadModelRepository(): RowReadModelRepository {
		return ExposedRowReadModelRepository()
	}

	@Provides
	@Singleton
	fun rowExtractor(chatGptHttpClient: ChatGptHttpClient): RowExtractor {
		return OpenAiExtractor(chatGptHttpClient.client, json)
	}
}
