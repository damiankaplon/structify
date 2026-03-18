package io.structify.domain.table

import io.structify.domain.table.model.Column
import java.util.*

data class CreateTableVersionCommand(
	val userId: UUID,
	val tableId: UUID,
	val columns: List<Column.Definition>,
)

class TableCommandHandler(
	private val tableRepository: TableRepository,
) {

	/**
	 * Loads the table, creates a new version (with domain validation),
	 * persists the updated aggregate, and returns the TableVersionCreated
	 * event.
	 *
	 * The caller is responsible for passing the returned
	 * event to the appropriate event handler (e.g.,
	 * TableVersionCreatedReadModelEventHandler).
	 */
	suspend fun handle(command: CreateTableVersionCommand): TableVersionCreated {
		val table = tableRepository.findByIdThrow(command.userId, command.tableId)
		table.update(command.columns)
		tableRepository.persist(table)
		return TableVersionCreated(
			tableId = table.id,
			userId = table.userId,
			version = table.getCurrentVersion(),
		)
	}
}
