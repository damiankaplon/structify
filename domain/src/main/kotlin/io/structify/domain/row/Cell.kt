package io.structify.domain.row

import java.util.UUID

data class Cell(
	val columnDefinitionId: UUID,
	var value: String,
)
