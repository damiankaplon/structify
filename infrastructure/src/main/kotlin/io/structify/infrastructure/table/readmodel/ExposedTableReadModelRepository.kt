package io.structify.infrastructure.table.readmodel

import io.structify.infrastructure.table.readmodel.TableReadModelRepository.Table
import io.structify.infrastructure.table.readmodel.persistence.TableReadModelTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.*

class ExposedTableReadModelRepository : TableReadModelRepository {

	override suspend fun findAllByUserId(userId: UUID): Set<Table> {
		return TableReadModelTable.selectAll()
			.where { TableReadModelTable.userId eq userId }
			.mapTo(linkedSetOf()) { row ->
				Table(
					id = row[TableReadModelTable.id].toString(),
					name = row[TableReadModelTable.name],
					description = row[TableReadModelTable.description]
				)
			}
	}

	override suspend fun create(id: UUID, userId: UUID, name: String, description: String) {
		TableReadModelTable.insert { row ->
			row[TableReadModelTable.id] = id
			row[TableReadModelTable.userId] = userId
			row[TableReadModelTable.name] = name
			row[TableReadModelTable.description] = description
		}
	}
}
