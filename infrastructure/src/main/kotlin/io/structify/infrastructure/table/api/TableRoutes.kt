package io.structify.infrastructure.table.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.reflect.typeInfo
import io.structify.domain.db.TransactionalRunner
import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.Column
import io.structify.domain.table.model.ColumnType
import io.structify.domain.table.model.StringFormat
import io.structify.domain.table.model.Table
import io.structify.infrastructure.security.jwtPrincipalOrThrow
import io.structify.infrastructure.table.readmodel.TableReadModelRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import kotlinx.serialization.Serializable
import java.util.UUID

fun Route.tableRoutes(
	transactionalRunner: TransactionalRunner,
	tableRepository: TableRepository,
	versionReadModelRepository: VersionReadModelRepository,
	tableReadModelRepository: TableReadModelRepository,
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

				val table = Table(
					userId = principal.userId,
					name = request.name,
				)

				tableRepository.persist(table)
				tableReadModelRepository.addDescription(table.id, request.description)

				call.respond(HttpStatusCode.Created, message = TableId(table.id.toString()))
			}

		}

		post("/{tableId}/versions") {
			transactionalRunner.transaction {
				val principal = call.jwtPrincipalOrThrow()
				val tableId = UUID.fromString(call.parameters["tableId"])
				val request = call.receive<List<ColumnDefinitionDto>>()

				val table = tableRepository.findByIdThrow(principal.userId, tableId)

				val columns: List<Column.Definition> = request.map(ColumnDefinitionDto::toDomain)
				table.update(columns)
				tableRepository.persist(table)

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
	val optional: Boolean,
) {

	fun toDomain(): Column.Definition {
		val domainType: ColumnType = when (type.uppercase()) {
			"STRING" -> ColumnType.StringType(format = stringFormat?.let { StringFormat.valueOf(it) })
			"NUMBER" -> ColumnType.NumberType
			else -> error("Unknown column type: $type")
		}
		return Column.Definition(
			name = name,
			description = description,
			type = domainType,
			optional = optional
		)
	}
}
