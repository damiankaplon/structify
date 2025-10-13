package io.structify.infrastructure.kotlinx.serialization

import kotlinx.serialization.Serializable
import java.util.*

@Serializable(with = UuidSerializer::class)
data class Uuid(val value: String) {
    constructor(uuid: UUID) : this(uuid.toString())
    fun toJava(): UUID = UUID.fromString(value)
    override fun toString(): String = this.value
}

fun UUID.toKotlinx(): Uuid = Uuid(toString())