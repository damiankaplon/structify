package io.structify.domain.databind

fun interface Serializer<T> {

    operator fun invoke(value: T): String
}
