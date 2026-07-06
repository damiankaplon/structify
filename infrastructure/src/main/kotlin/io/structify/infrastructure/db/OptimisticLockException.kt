package io.structify.infrastructure.db

/**
 * Thrown when a persist attempt loses an optimistic-lock race: the
 * aggregate root still exists but its stored opt-lock counter no longer
 * matches the one held in memory, meaning another transaction modified it
 * in the meantime.
 */
class OptimisticLockException(entity: String, id: Any) :
    RuntimeException("Optimistic lock conflict for $entity with id $id")
