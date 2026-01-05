package io.structify.infrastructure.row.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.row.Cell
import io.structify.domain.row.Row
import io.structify.domain.row.RowRepository
import io.structify.domain.table.TableRepository
import io.structify.infrastructure.row.readmodel.RowReadModelRepository
import io.structify.infrastructure.security.jwtPrincipalOrThrow
import kotlinx.serialization.Serializable
import java.util.UUID

fun Route.rowRoutes(
	transactionalRunner: TransactionalRunner,
	rowRepository: RowRepository,
	rowReadModelRepository: RowReadModelRepository,
	tableRepository: TableRepository,
) {
	route("/tables/{tableId}/rows") {
		get {
			transactionalRunner.transaction(readOnly = true) {
				val principal = call.jwtPrincipalOrThrow()
				val tableId = UUID.fromString(call.parameters["tableId"])

				// Verify user has access to this table
				tableRepository.findByIdThrow(principal.userId, tableId)

				val rows = rowReadModelRepository.findAllByTableId(tableId)

				call.respond(HttpStatusCode.OK, rows)
			}
		}

		post {
			transactionalRunner.transaction {
				val principal = call.jwtPrincipalOrThrow()
				val tableId = UUID.fromString(call.parameters["tableId"])
				val request = call.receive<CreateRowRequest>()

				// Verify user has access to this table
				tableRepository.findByIdThrow(principal.userId, tableId)

				val row = Row(
					tableId = tableId,
					cells = request.cells.mapTo(linkedSetOf()) { it.toDomain() },
				)

				rowRepository.save(row)

				call.respond(HttpStatusCode.Created, RowId(row.id.toString()))
			}
		}

		put("/{rowId}") {
			transactionalRunner.transaction {
				val principal = call.jwtPrincipalOrThrow()
				val tableId = UUID.fromString(call.parameters["tableId"])
				val rowId = UUID.fromString(call.parameters["rowId"])
				val request = call.receive<UpdateRowRequest>()

				// Verify user has access to this table
				tableRepository.findByIdThrow(principal.userId, tableId)

				val row = Row(
					id = rowId,
					tableId = tableId,
					cells = request.cells.mapTo(linkedSetOf()) { it.toDomain() },
				)

				rowRepository.save(row)

				call.respond(HttpStatusCode.OK)
			}
		}
	}
}

@Serializable
data class RowId(
	val id: String,
)

@Serializable
internal data class CreateRowRequest(
	val cells: Set<CellDto>,
)

@Serializable
internal data class UpdateRowRequest(
	val cells: Set<CellDto>,
)

@Serializable
internal data class CellDto(
	val columnId: Int,
	val value: String,
) {

	fun toDomain(): Cell {
		return Cell(
			columnId = columnId,
			value = value,
		)
	}
}
