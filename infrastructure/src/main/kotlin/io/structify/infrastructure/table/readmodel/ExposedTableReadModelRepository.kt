package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.table.persistence.TablesTable
import io.structify.infrastructure.table.readmodel.TableReadModelRepository.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedTableReadModelRepository : TableReadModelRepository {

	override suspend fun findAllByUserId(userId: UUID): Set<Table> {
		return TablesTable.selectAll()
			.where { TablesTable.userId eq userId }
			.mapTo(linkedSetOf()) { row ->
				Table(
					id = row[TablesTable.id].toString(),
					name = row[TablesTable.name],
					description = row[TablesTable.description]
				)
			}
	}

	override suspend fun addDescription(id: UUID, description: String) {
		TablesTable.update({ TablesTable.id eq id }) {
			it[TablesTable.description] = description
		}
	}
}
