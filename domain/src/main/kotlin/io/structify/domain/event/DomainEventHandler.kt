package io.structify.domain.event

/**
 * Handles a specific event type. Implementations live in infrastructure
 * and react to events (e.g., domain handlers creating aggregates, read
 * model handlers updating projections).
 */
interface DomainEventHandler<in T : DomainEvent> {
	suspend fun handle(event: T)
}
