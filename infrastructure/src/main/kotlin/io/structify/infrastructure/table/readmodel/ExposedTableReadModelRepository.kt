package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.table.persistence.TablesTable
import io.structify.infrastructure.table.readmodel.TableReadModelRepository.Table
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class ExposedTableReadModelRepository : TableReadModelRepository {

	override suspend fun findAllByUserId(userId: UUID): Set<Table> {
		return TablesTable.selectAll()
			.where { TablesTable.userId eq userId }
			.mapTo(linkedSetOf()) { row ->
				Table(
					id = row[TablesTable.id].toString(),
					name = row[TablesTable.name],
				)
			}
	}
}
