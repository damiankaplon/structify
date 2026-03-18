package io.structify.infrastructure.table.event

import io.structify.domain.event.DomainEventHandler
import io.structify.domain.table.TableCreated
import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.Table

/**
 * Consumes TableCreated event (emitted by CRUD/read model layer). Creates
 * the domain Table aggregate so it's available for subsequent domain
 * operations (version creation, row extraction).
 *
 * Direction: Read Model → Domain
 */
class TableCreatedDomainEventHandler(
	private val tableRepository: TableRepository,
) : DomainEventHandler<TableCreated> {

	override suspend fun handle(event: TableCreated) {
		val table = Table(
			id = event.tableId,
			userId = event.userId,
			name = event.name,
		)
		tableRepository.persist(table)
	}
}
