package io.structify.infrastructure.row.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.structify.domain.row.Cell
import io.structify.domain.table.model.Table
import io.structify.infrastructure.row.readmodel.RowReadModelRepository
import io.structify.infrastructure.test.clientJson
import io.structify.infrastructure.test.setupTestApp
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

internal class RowRoutesIntegrationTest {

	@Test
	fun `should create row`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()
		val table = Table(
			id = tableId,
			userId = loggedInUserUuid,
			name = "Test table"
		)
		testApp.tableRepository().persist(table)

		// when
		val response = clientJson.post("/api/tables/$tableId/rows") {
			contentType(ContentType.Application.Json)
			setBody(
				CreateRowRequest(
					cells = setOf(
						CellDto(columnId = 1, value = "test@test.com")
					)
				)
			)
		}

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.Created)
		val rowId = response.body<RowId>()
		assertThat(rowId.id).isNotNull()

		val rows = testApp.rowRepository().findAll()
		assertThat(rows).hasSize(1)
		val savedRow = rows.first()
		assertThat(savedRow.tableId).isEqualTo(tableId)
		assertThat(savedRow.cells).containsExactlyInAnyOrder(Cell(columnId = 1, value = "test@test.com"))
	}

	@Test
	fun `should update row`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()
		val table = Table(
			id = tableId,
			userId = loggedInUserUuid,
			name = "Test table"
		)
		testApp.tableRepository().persist(table)

		val rowId = UUID.randomUUID()
		val row = io.structify.domain.row.Row(
			id = rowId,
			tableId = tableId,
			cells = linkedSetOf(Cell(columnId = 1, value = "initial@test.com"))
		)
		testApp.rowRepository().save(row)

		// when
		val response = clientJson.put("/api/tables/$tableId/rows/$rowId") {
			contentType(ContentType.Application.Json)
			setBody(
				UpdateRowRequest(
					cells = setOf(
						CellDto(columnId = 1, value = "updated@test.com")
					)
				)
			)
		}

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.OK)

		val updatedRow = testApp.rowRepository().findById(rowId)
		assertThat(updatedRow).isNotNull
		assertThat(updatedRow!!.cells).containsExactlyInAnyOrder(Cell(columnId = 1, value = "updated@test.com"))
	}

	@Test
	fun `should get all rows for a table`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()
		val table = Table(
			id = tableId,
			userId = loggedInUserUuid,
			name = "Test table"
		)
		testApp.tableRepository().persist(table)

		val row1 = RowReadModelRepository.Row(
			id = UUID.randomUUID().toString(),
			cells = setOf(RowReadModelRepository.Cell(columnId = 1, value = "test1@test.com"))
		)
		val row2 = RowReadModelRepository.Row(
			id = UUID.randomUUID().toString(),
			cells = setOf(RowReadModelRepository.Cell(columnId = 1, value = "test2@test.com"))
		)
		testApp.rowReadModelRepository().addRow(tableId, row1)
		testApp.rowReadModelRepository().addRow(tableId, row2)

		// when
		val response = clientJson.get("/api/tables/$tableId/rows")

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.OK)
		val rows = response.body<List<RowReadModelRepository.Row>>()
		assertThat(rows).hasSize(2)
		assertThat(rows).containsExactlyInAnyOrder(row1, row2)
	}

	@Test
	fun `should return 404 when creating row for table user has no access to`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		val otherUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()
		val table = Table(
			id = tableId,
			userId = otherUserUuid, // table belongs to another user
			name = "Test table"
		)
		testApp.tableRepository().persist(table)

		// when
		val response = clientJson.post("/api/tables/$tableId/rows") {
			contentType(ContentType.Application.Json)
			setBody(
				CreateRowRequest(
					cells = setOf(
						CellDto(columnId = 1, value = "test@test.com")
					)
				)
			)
		}

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
	}
}
