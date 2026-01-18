package io.structify.infrastructure.kotlinx.serialization

import kotlinx.serialization.json.Json

val json = Json {
	encodeDefaults = true
	isLenient = true
	allowSpecialFloatingPointValues = true
	allowStructuredMapKeys = true
	prettyPrint = true
}
