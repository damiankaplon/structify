package io.structify.java

import java.util.UUID

fun String?.toUUID(): UUID = UUID.fromString(this)