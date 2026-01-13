package io.structify.domain.row

import java.util.UUID

class Row(
	val id: UUID = UUID.randomUUID(),
	val versionId: UUID,
	val cells: Set<Cell>,
)
