package io.structify.infrastructure.table.event

import io.structify.domain.event.DomainEventHandler
import io.structify.domain.table.TableVersionCreated
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository

/**
 * Consumes TableVersionCreated event (emitted by domain). Projects version
 * and column data into read model tables.
 *
 * Direction: Domain → Read Model
 */
class TableVersionCreatedReadModelEventHandler(
	private val versionReadModelRepository: VersionReadModelRepository,
) : DomainEventHandler<TableVersionCreated> {

	override suspend fun handle(event: TableVersionCreated) {
		versionReadModelRepository.upsertVersion(
			tableId = event.tableId,
			userId = event.userId,
			version = event.version,
		)
	}
}
