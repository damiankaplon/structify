package io.structify.infrastructure.table.api

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.table.CreateTableVersionCommand
import io.structify.domain.table.TableCommandHandler
import io.structify.domain.table.TableCreated
import io.structify.domain.table.TableVersionCreated
import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.infrastructure.security.jwtPrincipalOrThrow
import io.structify.infrastructure.table.event.TableCreatedDomainEventHandler
import io.structify.infrastructure.table.event.TableVersionCreatedReadModelEventHandler
import io.structify.infrastructure.table.readmodel.TableReadModelRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import kotlinx.serialization.Serializable
import java.util.*

fun Route.tableRoutes(
	transactionalRunner: TransactionalRunner,
	tableCommandHandler: TableCommandHandler,
	versionReadModelRepository: VersionReadModelRepository,
	tableReadModelRepository: TableReadModelRepository,
	tableCreatedDomainEventHandler: TableCreatedDomainEventHandler,
	tableVersionCreatedReadModelEventHandler: TableVersionCreatedReadModelEventHandler,
) {
	route("/tables") {
		get {
			transactionalRunner.transaction(readOnly = true) {
				val principal = call.jwtPrincipalOrThrow()

				val tables = tableReadModelRepository.findAllByUserId(principal.userId)

				call.respond(HttpStatusCode.OK, tables)
			}
		}
		post {
			transactionalRunner.transaction {
				val principal = call.jwtPrincipalOrThrow()
				val request = call.receive<CreateTableRequest>()

				val tableId = UUID.randomUUID()

				// Step 1: CRUD operation — store in read model
				tableReadModelRepository.create(tableId, principal.userId, request.name, request.description)

				// Step 2: Pass event to domain handler — creates the Table aggregate
				tableCreatedDomainEventHandler.handle(
					TableCreated(
						tableId = tableId,
						userId = principal.userId,
						name = request.name,
					)
				)

				call.respond(HttpStatusCode.Created, message = TableId(tableId.toString()))
			}
		}

		post("/{tableId}/versions") {
			transactionalRunner.transaction {
				val principal = call.jwtPrincipalOrThrow()
				val tableId = UUID.fromString(call.parameters["tableId"])
				val request = call.receive<List<ColumnDefinitionDto>>()
				val columns = request.map(ColumnDefinitionDto::toDomain)

				// Step 1: Domain operation — validation + state change, returns event
				val event: TableVersionCreated = tableCommandHandler.handle(
					CreateTableVersionCommand(
						userId = principal.userId,
						tableId = tableId,
						columns = columns,
					)
				)

				// Step 2: Pass event to read model handler — projects version data
				tableVersionCreatedReadModelEventHandler.handle(event)

				call.respond(HttpStatusCode.Created)
			}
		}

		get("/{tableId}/versions/current") {
			transactionalRunner.transaction(readOnly = true) {
				val principal = call.jwtPrincipalOrThrow()
				val tableId = UUID.fromString(call.parameters["tableId"])

				val version = versionReadModelRepository.findCurrentVersionByTableId(principal.userId, tableId)

				call.respond(HttpStatusCode.OK, version, typeInfo<Version?>())
			}
		}

		get("/{tableId}/versions") {
			transactionalRunner.transaction(readOnly = true) {
				val principal = call.jwtPrincipalOrThrow()
				val tableId = UUID.fromString(call.parameters["tableId"])

				val versions = versionReadModelRepository.findAllVersionsByTableId(principal.userId, tableId)

				call.respond(HttpStatusCode.OK, versions)
			}
		}
	}
}

@Serializable
data class TableId(
	val id: String,
)

@Serializable
internal data class CreateTableRequest(
	val name: String,
	val description: String,
)

@Serializable
internal data class ColumnDefinitionDto(
	val name: String,
	val description: String,
	val type: String,
	val stringFormat: String? = null,
	val optional: Boolean = false,
	val children: List<ColumnDefinitionDto> = emptyList(),
) {

	fun toDomain(): Column.Definition {
		val domainType: ColumnType = when (type.uppercase()) {
			"STRING" -> ColumnType.StringType(format = stringFormat?.let { StringFormat.valueOf(it) })
			"NUMBER" -> ColumnType.NumberType
			"OBJECT" -> ColumnType.ObjectType
			else -> error("Unknown column type: $type")
		}
		return Column.Definition(
			name = name,
			description = description,
			type = domainType,
			optional = optional,
			children = children.map { it.toDomain() }
		)
	}
}
