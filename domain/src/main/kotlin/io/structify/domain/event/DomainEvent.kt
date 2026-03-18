package io.structify.domain.event

/**
 * Marker interface for all events in the system. Events represent facts
 * that happened and need to be consumed by other parts of the system.
 *
 * Events can flow in two directions:
 * 1. CRUD/read model → domain: e.g., TableCreated triggers domain
 *    aggregate creation
 * 2. Domain → read model: e.g., TableVersionCreated triggers read model
 *    projection update
 */
interface DomainEvent
