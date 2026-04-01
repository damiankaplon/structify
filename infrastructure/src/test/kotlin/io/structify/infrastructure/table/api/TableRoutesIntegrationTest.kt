package io.structify.infrastructure.table.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.structify.infrastructure.kotlinx.serialization.toKotlinx
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnDefinition
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.ColumnType
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository.Version
import io.structify.infrastructure.test.clientJson
import io.structify.infrastructure.test.setupTestApp
import org.assertj.core.api.Assertions.assertThat
import java.util.*
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
		clientJson.post("/api/tables") {
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
					ColumnDefinitionDto(
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
		assertThat(updatedTable.versions.first().columns.first().definition.name).isEqualTo("email")
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
						id = UUID.randomUUID().toKotlinx(),
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
						id = UUID.randomUUID().toKotlinx(),
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
						id = UUID.randomUUID().toKotlinx(),
						name = "email",
						description = "User email",
						type = ColumnType(type = "STRING", format = null),
						optional = false
					),
					ColumnDefinition(
						id = UUID.randomUUID().toKotlinx(),
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
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Users", description = "All users")
		)
		testApp.tableReadModelRepository().addTable(
			loggedInUserUuid,
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Orders", description = "All orders")
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
	fun `should restore a table version via API`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()
		val table = io.structify.domain.table.model.Table(id = tableId, userId = loggedInUserUuid, name = "Test table")
		testApp.tableRepository().persist(table)

		// Create version 1 with "name" column
		clientJson.post("/api/tables/$tableId/versions") {
			contentType(ContentType.Application.Json)
			setBody(
				listOf(
					ColumnDefinitionDto(
						name = "name",
						description = "Person name",
						type = "STRING",
						optional = false
					)
				)
			)
		}

		// Create version 2 with "email" column
		clientJson.post("/api/tables/$tableId/versions") {
			contentType(ContentType.Application.Json)
			setBody(
				listOf(
					ColumnDefinitionDto(
						name = "email",
						description = "Person email",
						type = "STRING",
						optional = false
					)
				)
			)
		}

		// when — restore version 1
		val response = clientJson.post("/api/tables/$tableId/versions/1/restore")

		// then
		assertThat(response.status).isEqualTo(HttpStatusCode.Created)

		// domain aggregate has 3 versions now
		val updatedTable = testApp.tableRepository().findAll().first()
		assertThat(updatedTable.versions).hasSize(3)
		val restoredVersion = updatedTable.getCurrentVersion()
		assertThat(restoredVersion.orderNumber).isEqualTo(3)
		assertThat(restoredVersion.columns.map { it.definition.name }).containsExactly("name")

		// read model has the new version projected
		val readModelVersions = testApp.versionReadModelRepository().findAllVersionsByTableId(loggedInUserUuid, tableId)
		assertThat(readModelVersions).hasSize(3)
		val readModelRestored = readModelVersions.maxBy { it.orderNumber }
		assertThat(readModelRestored.orderNumber).isEqualTo(3)
		assertThat(readModelRestored.columns.map { it.name }).containsExactly("name")
	}

	@Test
	fun `should return error when restoring non-existent version`() = testApplication {
		// given
		val testApp = setupTestApp()
		val loggedInUserUuid = UUID.randomUUID()
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(loggedInUserUuid.toString())

		val tableId = UUID.randomUUID()
		val table = io.structify.domain.table.model.Table(id = tableId, userId = loggedInUserUuid, name = "Test table")
		testApp.tableRepository().persist(table)

		clientJson.post("/api/tables/$tableId/versions") {
			contentType(ContentType.Application.Json)
			setBody(
				listOf(
					ColumnDefinitionDto(
						name = "name",
						description = "Person name",
						type = "STRING",
						optional = false
					)
				)
			)
		}

		// when — restore version order number that doesn't exist
		val response = clientJson.post("/api/tables/$tableId/versions/999/restore")

		// then
		assertThat(response.status.value).isGreaterThanOrEqualTo(400)
	}

	@Test
	fun `should not allow restoring version of another user's table`() = testApplication {
		// given
		val testApp = setupTestApp()
		val userAUuid = UUID.randomUUID()
		val userBUuid = UUID.randomUUID()

		// table belongs to user A
		val tableId = UUID.randomUUID()
		val table = io.structify.domain.table.model.Table(id = tableId, userId = userAUuid, name = "User A table")
		testApp.tableRepository().persist(table)
		// set auth as user A to create a version
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(userAUuid.toString())
		clientJson.post("/api/tables/$tableId/versions") {
			contentType(ContentType.Application.Json)
			setBody(
				listOf(
					ColumnDefinitionDto(
						name = "name",
						description = "Person name",
						type = "STRING",
						optional = false
					)
				)
			)
		}

		// authenticate as user B
		testApp.mockJwtAuthenticationProvider().setTestJwtPrincipalSubject(userBUuid.toString())

		// when — user B tries to restore user A's table version
		val response = clientJson.post("/api/tables/$tableId/versions/1/restore")

		// then — multitenancy enforcement returns 404
		assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
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
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Customers", description = "All customers")
		)
		testApp.tableReadModelRepository().addTable(
			loggedInUserUuid,
			TableReadModelTable(id = UUID.randomUUID().toString(), name = "Invoices", description = "All invoices")
		)

		// and one table for a different user
		testApp.tableReadModelRepository().addTable(
			otherUserUuid,
			TableReadModelTable(
				id = UUID.randomUUID().toString(),
				name = "ShouldNotBeVisible",
				description = "Should not be visible"
			)
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
