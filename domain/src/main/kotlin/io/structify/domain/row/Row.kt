package io.structify.domain.row

import java.util.*

class Row(
	val id: UUID = UUID.randomUUID(),
	val versionId: UUID,
	val cells: Set<Cell>,
    /** Optimistic-lock counter, managed exclusively by the persistence layer. */
    var optLock: Long = 0,
)
