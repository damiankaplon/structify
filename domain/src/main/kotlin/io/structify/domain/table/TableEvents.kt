package io.structify.domain.table

import io.structify.domain.event.DomainEvent
import io.structify.domain.table.model.Version
import java.util.*

/**
 * Emitted by the read model (CRUD layer) when a new table is created.
 * Consumed by the domain to create the Table aggregate.
 *
 * Direction: Read Model → Domain
 *
 * This event carries only the data the domain needs to create its
 * aggregate. The `name` field is included because the domain Table
 * has a `name` property, even though no current domain logic uses it.
 */
data class TableCreated(
	val tableId: UUID,
	val userId: UUID,
	val name: String,
) : DomainEvent

/**
 * Emitted by the domain when a new Version is added to a Table via
 * table.update(). Consumed by the read model to project version data into
 * read model tables.
 *
 * Direction: Domain → Read Model
 *
 * Carries the full version with its column hierarchy so the read model can
 * project it without reading from domain tables.
 */
data class TableVersionCreated(
	val tableId: UUID,
	val userId: UUID,
	val version: Version,
) : DomainEvent
