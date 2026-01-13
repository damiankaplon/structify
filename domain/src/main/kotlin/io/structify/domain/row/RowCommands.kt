package io.structify.domain.row

import io.structify.domain.table.TableRepository
import java.util.UUID

data class ExtractRowCommand(
	val tableId: UUID,
	val versionNumber: Int,
	val content: String,
)

class RowCommandHandler(
	private val tableRepository: TableRepository,
	private val rowRepository: RowRepository,
	private val currentlyAuthenticatedUserIdProvider: () -> UUID,
	private val rowExtractor: RowExtractor,
) {

	suspend fun handle(cmd: ExtractRowCommand) {
		val userId = currentlyAuthenticatedUserIdProvider()
		val table = tableRepository.findByIdThrow(userId, cmd.tableId)
		val version = table.versions.find { it.orderNumber == cmd.versionNumber }
			?: error("$table does not have a version with number ${cmd.versionNumber}.")
		val cells = rowExtractor.extract(version.columns, cmd.content)
		val row = Row(versionId = version.id, cells = cells)
		rowRepository.save(row)
	}
}
