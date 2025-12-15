package io.structify.infrastructure.table.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnDefinition
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnType
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import io.structify.infrastructure.test.clientJson
import io.structify.infrastructure.test.setupTestApp
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test
import io.structify.infrastructure.table.readmodel.TableReadModelRepository.Table as TableReadModelTable
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version as VersionReadModel

internal class TableRoutesIntegrationTest {

	@Test
	fun `should create table`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		// when
		val response = clientJson.post("/api/tables") {
			contentType(ContentType.Application.Json)
			setBody(
				CreateTableRequest(
					name = "Test table",
					description = "Some description"
				)
			)
		}

		// then
		val tables = testApp.tableRepository().findAll()
		assertThat(tables).satisfiesExactlyInAnyOrder(
			{
				assertThat(it.name).isEqualTo("Test table")
				assertThat(it.userId).isEqualTo(loggedInUserUuid)
			}
		)
	}

	@Test
	fun `should create version for table`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()
		val table = io.structify.domain.table.model.Table(
			id = tableId,
			userId = loggedInUserUuid,
			name = "Test table"
		)
		testApp.tableRepository().persist(table)

		// when
		val response = clientJson.post("/api/tables/$tableId/versions") {
			contentType(ContentType.Application.Json)
			setBody(
				listOf(
					ColumnDto(
						name = "email",
						description = "User email",
						type = "STRING",
						stringFormat = null,
						optional = false
					)
				)
			)
		}

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.Created)
		val updatedTable = testApp.tableRepository().findAll().first()
		assertThat(updatedTable.versions).hasSize(1)
		assertThat(updatedTable.versions.first().columns).hasSize(1)
		assertThat(updatedTable.versions.first().columns.first().name).isEqualTo("email")
	}

	@Test
	fun `should get current version for table`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()

		testApp.versionReadModelRepository().addVersion(
			loggedInUserUuid,
			tableId,
			Version(
				id = UUID.randomUUID().toString(),
				columns = listOf(
					ColumnDefinition(
						name = "email",
						description = "User email",
						type = ColumnType(type = "STRING", format = null),
						optional = false
					)
				),
				orderNumber = 1
			)
		)

		// when
		val response = clientJson.get("/api/tables/$tableId/versions/current")

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.OK)
		val version = response.body<VersionReadModel>()
		assertThat(version.orderNumber).isEqualTo(1)
		assertThat(version.columns).hasSize(1)
		assertThat(version.columns.first().name).isEqualTo("email")
	}

	@Test
	fun `should get all versions for table`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()

		testApp.versionReadModelRepository().addVersion(
			loggedInUserUuid,
			tableId,
			Version(
				id = UUID.randomUUID().toString(),
				columns = listOf(
					ColumnDefinition(
						name = "email",
						description = "User email",
						type = ColumnType(type = "STRING", format = null),
						optional = false
					)
				),
				orderNumber = 1
			)
		)

		testApp.versionReadModelRepository().addVersion(
			loggedInUserUuid,
			tableId,
			Version(
				id = UUID.randomUUID().toString(),
				columns = listOf(
					ColumnDefinition(
						name = "email",
						description = "User email",
						type = ColumnType(type = "STRING", format = null),
						optional = false
					),
					ColumnDefinition(
						name = "age",
						description = "User age",
						type = ColumnType(type = "NUMBER", format = null),
						optional = true
					)
				),
				orderNumber = 2
			)
		)

		// when
		val response = clientJson.get("/api/tables/$tableId/versions")

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.OK)
		val versions = response.body<List<VersionReadModel>>()
		assertThat(versions).hasSize(2)
		assertThat(versions.map { it.orderNumber }).containsExactlyInAnyOrder(1, 2)
	}

	@Test
	fun `should get all tables for current user`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		// and two tables for this user in read model
		testApp.tableReadModelRepository().addTable(
			loggedInUserUuid,
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Users")
		)
		testApp.tableReadModelRepository().addTable(
			loggedInUserUuid,
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Orders")
		)

		// when
		val response = clientJson.get("/api/tables")

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.OK)
		val tables = response.body<List<TableReadModelTable>>()
		assertThat(tables).hasSize(2)
		assertThat(tables.map { it.name }).containsExactlyInAnyOrder("Users", "Orders")
	}

	@Test
	fun `should not return tables belonging to other users`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		val otherUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		// and two tables for current user
		testApp.tableReadModelRepository().addTable(
			loggedInUserUuid,
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Customers")
		)
		testApp.tableReadModelRepository().addTable(
			loggedInUserUuid,
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Invoices")
		)

		// and one table for a different user
		testApp.tableReadModelRepository().addTable(
			otherUserUuid,
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "ShouldNotBeVisible")
		)

		// when
		val response = clientJson.get("/api/tables")

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.OK)
		val tables = response.body<List<TableReadModelTable>>()
		assertThat(tables).hasSize(2)
		assertThat(tables.map { it.name }).containsExactlyInAnyOrder("Customers", "Invoices")
		assertThat(tables.map { it.name }).doesNotContain("ShouldNotBeVisible")
	}
}
