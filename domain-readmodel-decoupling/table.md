# Table Read Model Decoupling from Domain Model Database

## Problem Statement

The `ExposedTableReadModelRepository` and `VersionReadModelExposedRepository` in the `infrastructure` module
currently read directly from the **domain model database tables** (`TablesTable`, `TableVersionsTable`,
`TableColumnsTable`, `VersionColumnTable`). This violates the CQRS architectural rule stated in `BACKEND.md`:

> *"Read model must not read any data from a domain model."*

Additionally, `ExposedTableReadModelRepository.addDescription()` **writes** to the domain `TablesTable`,
storing CRUD-only data (`description`) inside the domain model table. The `description` field exists only for
user display purposes and does not participate in any domain logic — the domain `Table` class does not even
have a `description` property.

### Summary of violations

| Repository                                                        | Violation                                                                                                     |
|-------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `ExposedTableReadModelRepository.findAllByUserId()`               | Reads from `TablesTable` (domain table)                                                                       |
| `ExposedTableReadModelRepository.addDescription()`                | Writes to `TablesTable` (domain table)                                                                        |
| `VersionReadModelExposedRepository.findAllVersionsByTableId()`    | Reads from `TableVersionsTable`, `TablesTable`, `TableColumnsTable`, `VersionColumnTable` (all domain tables) |
| `VersionReadModelExposedRepository.findCurrentVersionByTableId()` | Same as above                                                                                                 |

---

## Subdomain

**Table subdomain** (`domain/src/main/kotlin/io/structify/domain/table/`)

This subdomain is responsible for defining table structures (columns, versions) that drive data extraction
from uploaded documents. The read model provides user-facing views of these structures.

---

## How This Fits Into the Existing Architecture

The project follows hexagonal architecture with CQRS. Per `BACKEND.md`:

- **Domain models** (aggregates, entities, value objects) hold only data required for command processing and
  domain constraint validations.
- **Read models** are separate views for user read purposes and must store their data independently.
- When a problem has both CRUD and processing characteristics:
    1. **CRUD-read model emitting events** that are consumed by domain model creating aggregates/entities.
    2. **Domain model emitting events** that are consumed by read model to update its state.
- Within the same bounded context, domain logic and read model event consumption can happen in the same
  transaction.

Currently the domain `Table` aggregate:

- Has properties: `id`, `userId`, `name`, `versions` (with columns).
- Does **not** have `description` — this is purely a read-model concern.
- Is persisted via `ExposedTableRepository` into `TablesTable`, `TableVersionsTable`, `TableColumnsTable`,
  `VersionColumnTable`.
- Does **not** emit any events — there is no event infrastructure in the codebase yet.

The `description` column was added to the domain `tables` database table by migration `V6__table_description.sql`
but it does not belong there — it should be in a read-model-specific table.

The existing `RowCommandHandler` already demonstrates the command handler pattern in the codebase, but
no event pattern exists yet. This work will introduce events.

### Key architectural insight: two different event directions

Looking at the table subdomain through the CQRS lens from `BACKEND.md`:

1. **Creating a table** (`POST /api/tables`) is fundamentally a **CRUD/read model operation**. The user
   provides `name` and `description` — both are display data. The `name` field, despite existing on the
   `Table` aggregate, is never used in any domain logic (no validation, no business rules reference it).
   The domain `Table` aggregate exists only because later domain operations (version creation, row extraction)
   need a table to work with. Therefore:
    - The **read model stores the table first** (CRUD operation).
    - The read model **emits a `TableCreated` event**.
    - The **domain consumes** this event to create the `Table` aggregate.

2. **Creating a version** (`POST /api/tables/{tableId}/versions`) is a **domain operation**. Column hierarchy
   validation (`ColumnHierarchyValidation.validate()`) is genuine domain logic. The `table.update(columns)`
   method enforces business rules. Therefore:
    - The **domain processes the command** (validation + state change).
    - The domain **emits a `TableVersionCreated` event**.
    - The **read model consumes** this event to project version data.

This matches the two synchronization patterns from `BACKEND.md` exactly.

---

## Plan

### 1. Introduce event types in the `domain` module

#### 1a. Event marker interface

New file: `domain/src/main/kotlin/io/structify/domain/event/DomainEvent.kt`

```kotlin
package io.structify.domain.event

/**
 * Marker interface for all events in the system.
 * Events represent facts that happened and need to be consumed by other parts of the system.
 *
 * Events can flow in two directions:
 * 1. CRUD/read model → domain: e.g., TableCreated triggers domain aggregate creation
 * 2. Domain → read model: e.g., TableVersionCreated triggers read model projection update
 */
interface DomainEvent
```

#### 1b. Event handler interface

New file: `domain/src/main/kotlin/io/structify/domain/event/DomainEventHandler.kt`

```kotlin
package io.structify.domain.event

/**
 * Handles a specific event type.
 * Implementations live in infrastructure and react to events
 * (e.g., domain handlers creating aggregates, read model handlers updating projections).
 */
interface DomainEventHandler<in T : DomainEvent> {
    suspend fun handle(event: T)
}
```

No event publisher or dispatcher is needed. Since everything operates within a single bounded context
and the same transaction, callers (route handlers, command handlers) pass events directly to the
appropriate event handler instances.

### 2. Define table events

New file: `domain/src/main/kotlin/io/structify/domain/table/TableEvents.kt`

Two events are needed, each flowing in a different direction:

```kotlin
package io.structify.domain.table

import io.structify.domain.event.DomainEvent
import io.structify.domain.table.model.Version
import java.util.UUID

/**
 * Emitted by the read model (CRUD layer) when a new table is created.
 * Consumed by the domain to create the Table aggregate.
 *
 * Direction: Read Model → Domain
 *
 * This event carries only the data the domain needs to create its aggregate.
 * The `name` field is included because the domain Table has a `name` property,
 * even though no current domain logic uses it.
 */
data class TableCreated(
    val tableId: UUID,
    val userId: UUID,
    val name: String,
) : DomainEvent

/**
 * Emitted by the domain when a new Version is added to a Table via table.update().
 * Consumed by the read model to project version data into read model tables.
 *
 * Direction: Domain → Read Model
 *
 * Carries the full version with its column hierarchy so the read model
 * can project it without reading from domain tables.
 */
data class TableVersionCreated(
    val tableId: UUID,
    val userId: UUID,
    val version: Version,
) : DomainEvent
```

### 3. Create event handlers

#### 3a. Domain-side handler: `TableCreatedDomainEventHandler` (consumes read model event)

This handler lives in infrastructure (it needs the domain repository port) and creates the
domain `Table` aggregate when the read model emits `TableCreated`.

New file: `infrastructure/src/main/kotlin/io/structify/infrastructure/table/event/TableCreatedDomainEventHandler.kt`

```kotlin
package io.structify.infrastructure.table.event

import io.structify.domain.event.DomainEventHandler
import io.structify.domain.table.TableCreated
import io.structify.domain.table.TableRepository
import io.structify.domain.table.model.Table

/**
 * Consumes TableCreated event (emitted by CRUD/read model layer).
 * Creates the domain Table aggregate so it's available for subsequent
 * domain operations (version creation, row extraction).
 *
 * Direction: Read Model → Domain
 */
class TableCreatedDomainEventHandler(
    private val tableRepository: TableRepository,
) : DomainEventHandler<TableCreated> {

    override suspend fun handle(event: TableCreated) {
        val table = Table(
            id = event.tableId,
            userId = event.userId,
            name = event.name,
        )
        tableRepository.persist(table)
    }
}
```

#### 3b. Read model-side handler: `TableVersionCreatedReadModelEventHandler` (consumes domain event)

This handler lives in infrastructure and projects domain version data into the read model tables.

New file:
`infrastructure/src/main/kotlin/io/structify/infrastructure/table/event/TableVersionCreatedReadModelEventHandler.kt`

```kotlin
package io.structify.infrastructure.table.event

import io.structify.domain.event.DomainEventHandler
import io.structify.domain.table.TableVersionCreated
import io.structify.infrastructure.table.readmodel.VersionReadModelRepository

/**
 * Consumes TableVersionCreated event (emitted by domain).
 * Projects version and column data into read model tables.
 *
 * Direction: Domain → Read Model
 */
class TableVersionCreatedReadModelEventHandler(
    private val versionReadModelRepository: VersionReadModelRepository,
) : DomainEventHandler<TableVersionCreated> {

    override suspend fun handle(event: TableVersionCreated) {
        versionReadModelRepository.upsertVersion(
            tableId = event.tableId,
            userId = event.userId,
            version = event.version,
        )
    }
}
```

### 4. Create table version command handler (domain operation)

Version creation is a domain operation. It needs a command handler that orchestrates the domain logic
and returns the resulting events. This follows the existing `RowCommandHandler` pattern.

New file: `domain/src/main/kotlin/io/structify/domain/table/TableCommands.kt`

```kotlin
package io.structify.domain.table

import io.structify.domain.event.DomainEvent
import io.structify.domain.table.model.Column
import java.util.UUID

data class CreateTableVersionCommand(
    val userId: UUID,
    val tableId: UUID,
    val columns: List<Column.Definition>,
)

class TableCommandHandler(
    private val tableRepository: TableRepository,
) {

    /**
     * Loads the table, creates a new version (with domain validation),
     * persists the updated aggregate, and returns a TableVersionCreated event.
     *
     * The caller is responsible for passing the returned event to the appropriate
     * event handler (e.g., TableVersionCreatedReadModelEventHandler).
     */
    suspend fun handle(command: CreateTableVersionCommand): List<DomainEvent> {
        val table = tableRepository.findByIdThrow(command.userId, command.tableId)
        table.update(command.columns)
        tableRepository.persist(table)
        return listOf(
            TableVersionCreated(
                tableId = table.id,
                userId = table.userId,
                version = table.getCurrentVersion(),
            )
        )
    }
}
```

**Note:** There is no `CreateTableCommand` in the domain. Table creation is a CRUD operation handled
by the read model layer — the domain `Table` aggregate is created as a side effect via the
`TableCreatedDomainEventHandler` when the route handler passes it the `TableCreated` event.

### 5. Create dedicated read model database tables

Create a new Flyway migration introducing read-model-specific tables that mirror the data needed for display:

#### `table_read_model` table

```
table_read_model
├── id          UUID (PK) — matches domain table aggregate id
├── user_id     UUID NOT NULL
├── name        VARCHAR(255) NOT NULL
└── description TEXT DEFAULT ''
```

#### `version_read_model` table

```
version_read_model
├── id            UUID (PK) — matches domain version id
├── table_id      UUID NOT NULL (FK → table_read_model.id)
└── order_number  INTEGER NOT NULL
```

#### `column_read_model` table

```
column_read_model
├── id               UUID (PK) — matches domain column id
├── name             VARCHAR(255) NOT NULL
├── description      TEXT NOT NULL
├── type_name        VARCHAR(50) NOT NULL
├── string_format    VARCHAR(50) NULL
├── optional         BOOLEAN NOT NULL
└── parent_column_id UUID NULL (self-referencing FK)
```

#### `version_column_read_model` association table

```
version_column_read_model
├── version_id           UUID (FK → version_read_model.id)
└── column_definition_id UUID (FK → column_read_model.id)
```

**Note:** These read model tables deliberately duplicate data from the domain tables. This is by design
in CQRS — the read model is a projection of the domain state optimized for queries.

### 6. Create Exposed table definitions for read model

New file:
`infrastructure/src/main/kotlin/io/structify/infrastructure/table/readmodel/persistence/TableReadModelTables.kt`

Define Exposed table objects:

- `TableReadModelDbTable` (mapping to `table_read_model`)
- `VersionReadModelDbTable` (mapping to `version_read_model`)
- `ColumnReadModelDbTable` (mapping to `column_read_model`)
- `VersionColumnReadModelDbTable` (mapping to `version_column_read_model`)

### 7. Remove `description` column from domain `TablesTable`

Create a Flyway migration to:

1. Copy existing `description` values from `tables` to `table_read_model`.
2. Drop the `description` column from the `tables` table.

Update `infrastructure/src/main/kotlin/io/structify/infrastructure/table/persistence/Tables.kt`:

- Remove the `description` field from `TablesTable`.

### 8. Update the `TableReadModelRepository` interface

Current interface:

```kotlin
interface TableReadModelRepository {
    suspend fun findAllByUserId(userId: UUID): Set<Table>
    suspend fun addDescription(id: UUID, description: String)
}
```

New interface:

```kotlin
interface TableReadModelRepository {
    suspend fun findAllByUserId(userId: UUID): Set<Table>
    suspend fun create(id: UUID, userId: UUID, name: String, description: String)
}
```

The `addDescription` method is replaced with `create` because table creation is now a read model CRUD
operation — the read model stores the full table entry (`id`, `userId`, `name`, `description`) as the
first step before passing the `TableCreated` event to the domain handler.

### 9. Update the `VersionReadModelRepository` interface

Add a write method consumed by the event handler:

```kotlin
interface VersionReadModelRepository {
    // existing read methods...
    suspend fun upsertVersion(tableId: UUID, userId: UUID, version: Version)
}
```

### 10. Rewrite `ExposedTableReadModelRepository` to use read model tables

Modify `ExposedTableReadModelRepository`:

- `findAllByUserId()` — query `TableReadModelDbTable` instead of `TablesTable`.
- `create()` — insert into `TableReadModelDbTable` instead of updating `TablesTable`.

### 11. Rewrite `VersionReadModelExposedRepository` to use read model tables

Modify `VersionReadModelExposedRepository`:

- All read methods should query from `VersionReadModelDbTable`, `ColumnReadModelDbTable`,
  `VersionColumnReadModelDbTable`, and `TableReadModelDbTable` instead of the domain tables.
- Implement `upsertVersion()` writing to the read model tables.

### 12. Update `TableRoutes` to use the new event-driven flows

Current `TableRoutes.kt` manually creates domain objects and calls repositories directly.
The routes should now use the correct event-driven patterns for each operation.
Event handlers are injected as dependencies and called directly — no publisher infrastructure needed.

**POST `/api/tables` — table creation (CRUD → event → domain):**

```kotlin
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
```

Key changes:

1. The route handler generates the `tableId` (UUID) since it no longer delegates to the domain
   `Table` constructor.
2. The read model stores the table first (including `description` which is CRUD-only data).
3. The `TableCreated` event is passed directly to `TableCreatedDomainEventHandler`, which creates
   the domain `Table` aggregate with the same `tableId`.
4. All of this happens within the same database transaction.

**POST `/api/tables/{tableId}/versions` — version creation (domain → event → read model):**

```kotlin
post("/{tableId}/versions") {
    transactionalRunner.transaction {
        val principal = call.jwtPrincipalOrThrow()
        val tableId = UUID.fromString(call.parameters["tableId"])
        val request = call.receive<List<ColumnDefinitionDto>>()
        val columns = request.map(ColumnDefinitionDto::toDomain)

        // Step 1: Domain operation — validation + state change, returns events
        val events = tableCommandHandler.handle(
            CreateTableVersionCommand(
                userId = principal.userId,
                tableId = tableId,
                columns = columns,
            )
        )

        // Step 2: Pass events to read model handler — projects version data
        events.filterIsInstance<TableVersionCreated>().forEach { event ->
            tableVersionCreatedReadModelEventHandler.handle(event)
        }

        call.respond(HttpStatusCode.Created)
    }
}
```

### 13. Update `tableRoutes` function signature

The function signature changes to accept the new event handler dependencies:

```kotlin
fun Route.tableRoutes(
    transactionalRunner: TransactionalRunner,
    tableCommandHandler: TableCommandHandler,
    versionReadModelRepository: VersionReadModelRepository,
    tableReadModelRepository: TableReadModelRepository,
    tableCreatedDomainEventHandler: TableCreatedDomainEventHandler,
    tableVersionCreatedReadModelEventHandler: TableVersionCreatedReadModelEventHandler,
)
```

Note: `tableRepository: TableRepository` is removed from the signature — it is no longer used directly
by the route handler. The domain repository is used internally by `TableCommandHandler` and
`TableCreatedDomainEventHandler`.

### 14. Update Dagger modules

#### `TableRepositoryModule` updates:

- Provide `TableCommandHandler`.
- Provide `TableCreatedDomainEventHandler`.
- Provide `TableVersionCreatedReadModelEventHandler`.

### 15. Update `Application.kt` and `StructifyAppComponent`

Wire `TableCommandHandler`, `TableCreatedDomainEventHandler`, and `TableVersionCreatedReadModelEventHandler`
through the DI graph. Update `installApp()` and `installRouting()` signatures to accept the new dependencies.

### 16. Remove `TableReadModelSynchronizer`

The existing `TableReadModelSynchronizer` is a prior attempt at synchronization that uses direct
method calls rather than events. It should be deleted — its responsibilities are now covered by
the event handlers (`TableCreatedDomainEventHandler`, `TableVersionCreatedReadModelEventHandler`).

---

## Code Flow Descriptions

### Create Table (POST `/api/tables`)

**Input**: HTTP request with `{ name: "...", description: "..." }` + JWT principal (userId).

**Current flow**:

1. `TableRoutes.post("/tables")` receives request.
2. Creates domain `Table(userId, name)` directly in route handler.
3. `tableRepository.persist(table)` → writes to domain `tables` DB table.
4. `tableReadModelRepository.addDescription(table.id, description)` → writes `description` to domain
   `tables` DB table (**VIOLATION**).

**New flow**:

1. `TableRoutes.post("/tables")` receives request.
2. Route handler generates a new `tableId` (UUID).
3. Route handler calls `tableReadModelRepository.create(tableId, userId, name, description)`
   → **writes to `table_read_model` DB table** (CRUD operation on read model's own table).
4. Route handler calls `tableCreatedDomainEventHandler.handle(TableCreated(tableId, userId, name))`.
5. **`TableCreatedDomainEventHandler.handle()`** creates domain `Table(id=tableId, userId, name)` and
   calls `tableRepository.persist(table)` → **writes to domain `tables` DB table**.

**All of steps 2–5 happen within the same database transaction.**

**Output**: HTTP 201 with `{ id: "..." }`.

**Event direction**: Read Model → Domain (CRUD emits event consumed by domain).

### Create Version (POST `/api/tables/{tableId}/versions`)

**Input**: HTTP request with column definitions + JWT principal + tableId path param.

**Current flow**:

1. Load domain `Table` via `tableRepository.findByIdThrow()`.
2. `table.update(columns)` — domain logic creates new version.
3. `tableRepository.persist(table)` — persists to domain tables.
4. No read model synchronization happens. Read model reads directly from domain tables (**VIOLATION**).

**New flow**:

1. Route handler invokes `tableCommandHandler.handle(CreateTableVersionCommand(userId, tableId, columns))`.
2. **Inside command handler**: loads `Table`, calls `table.update(columns)` (domain validation), persists
   via `tableRepository.persist()` → writes to domain tables.
3. Command handler returns `[TableVersionCreated event]`.
4. Route handler passes each `TableVersionCreated` event to `tableVersionCreatedReadModelEventHandler.handle()`.
5. **`TableVersionCreatedReadModelEventHandler.handle()`** calls
   `versionReadModelRepository.upsertVersion(tableId, userId, version)` → writes version and column data
   to `version_read_model`, `column_read_model`, `version_column_read_model` DB tables.

**All of steps 1–5 happen within the same database transaction.**

**Output**: HTTP 201.

**Event direction**: Domain → Read Model (domain emits event consumed by read model).

### Get All Tables (GET `/api/tables`)

**Input**: JWT principal (userId).

**Current flow**: `tableReadModelRepository.findAllByUserId(userId)` → reads from domain `tables` table.

**New flow**: `tableReadModelRepository.findAllByUserId(userId)` → reads from `table_read_model` table.

**Output**: HTTP 200 with list of `{ id, name, description }`.

### Get Current Version (GET `/api/tables/{tableId}/versions/current`)

**Input**: JWT principal + tableId path param.

**Current flow**: `versionReadModelRepository.findCurrentVersionByTableId(userId, tableId)` → reads from
domain `table_versions`, `tables`, `table_columns`, `version_column_assoc`.

**New flow**: `versionReadModelRepository.findCurrentVersionByTableId(userId, tableId)` → reads from
`version_read_model`, `table_read_model`, `column_read_model`, `version_column_read_model`.

**Output**: HTTP 200 with version data.

### Get All Versions (GET `/api/tables/{tableId}/versions`)

Same pattern as "Get Current Version" but returns all versions.

---

## Event Flow Diagrams

### Table Creation (Read Model → Domain)

```
┌──────────────┐
│ TableRoutes   │
│   (infra)     │
│               │
│  1. Generate tableId (UUID)
│               │
│  2. tableReadModelRepository.create(tableId, userId, name, description)
│     └──► writes to table_read_model DB table (CRUD)
│               │
│  3. tableCreatedDomainEventHandler.handle(TableCreated(...))
│               │
└───────┬───────┘
        │ direct call
        ▼
┌──────────────────────────────────────┐
│ TableCreatedDomainEventHandler       │
│        (infra)                       │
│                                      │
│ Creates domain Table(id, userId, name)│
│ tableRepository.persist(table)       │
│ └──► writes to domain tables DB table│
└──────────────────────────────────────┘
```

### Version Creation (Domain → Read Model)

```
┌──────────────┐     CreateTableVersionCommand   ┌──────────────────────┐
│ TableRoutes   │ ─────────────────────────────►  │ TableCommandHandler  │
│   (infra)     │                                │      (domain)        │
│               │     [TableVersionCreated]       │                      │
│               │ ◄───────────────────────────── │ table.update(cols)   │
│               │                                │ tableRepository      │
│               │                                │   .persist(table)    │
│               │                                │ (writes to domain    │
│               │                                │  tables)             │
│               │                                └──────────────────────┘
│               │
│  tableVersionCreatedReadModelEventHandler.handle(event)
│               │
└───────┬───────┘
        │ direct call
        ▼
┌──────────────────────────────────────────────┐
│ TableVersionCreatedReadModelEventHandler     │
│        (infra)                               │
│                                              │
│ versionReadModelRepository                   │
│   .upsertVersion(tableId, userId, version)   │
│ (writes to read model tables)                │
└──────────────────────────────────────────────┘
```

---

## Aggregate Roots

### `Table` aggregate (existing — unchanged)

**Location:** `domain/src/main/kotlin/io/structify/domain/table/model/Table.kt`

The `Table` aggregate root is **not modified** by this work. It continues to:

- Hold `id`, `userId`, `name`, `versions`.
- Provide `update(definitions)` for creating new versions with column hierarchy validation.
- Provide `getCurrentVersion()` for reading the latest version.

**What changes around it:**

- Previously created directly in the route handler. Now created by `TableCreatedDomainEventHandler`
  in response to the `TableCreated` event.
- The `Table` constructor must accept a pre-generated `id` (it already does via the `id` parameter
  with a default value of `UUID.randomUUID()`).

### Read model "entities" (not domain aggregates — CRUD data)

These are simple data holders stored in read model tables. They have no business invariants:

- **`TableReadModelRepository.Table`** — `id`, `name`, `description`. Stored in `table_read_model`.
- **`VersionReadModelRepository.Version`** — `id`, `columns`, `orderNumber`. Projected from domain via events into
  `version_read_model` + `column_read_model` + `version_column_read_model`.

---

## Test Scenarios

### Domain Level Tests

#### Event infrastructure tests (new)

1. **`DomainEvent marker interface exists and can be implemented`**
    - Verify `TableCreated` and `TableVersionCreated` implement `DomainEvent`.

#### TableCommandHandler unit tests (new)

2. **`should create version and return TableVersionCreated event`**
    - Given an existing table in the (in-memory) repository and a `CreateTableVersionCommand`.
    - When `handle()` is called.
    - Then returns a `TableVersionCreated` event with the new version containing correct columns.
    - And the table with the new version is persisted in the repository.

3. **`should throw when creating version for non-existent table`**
    - Given a `CreateTableVersionCommand` with a non-existent tableId.
    - When `handle()` is called.
    - Then throws appropriate exception.

4. **`should validate column hierarchy when creating version`**
    - Given a `CreateTableVersionCommand` with invalid column hierarchy (e.g., non-OBJECT parent with children).
    - When `handle()` is called.
    - Then throws validation exception (existing domain validation still applies).

5. **`should increment version order number correctly`**
    - Given a table that already has version 1.
    - When `handle()` is called with new columns.
    - Then the returned `TableVersionCreated` event contains a version with `orderNumber = 2`.

#### Existing domain tests (unchanged)

All existing tests in `TableTest.kt`, `TableHierarchicalVersionTest.kt`, `ColumnHierarchyValidationTest.kt`
should continue to pass without modification — the `Table` aggregate itself is not changed.

### Infrastructure Level — Event Handler Tests

6. **`TableCreatedDomainEventHandler should create domain Table aggregate`**
    - Given a `TableCreated` event with `tableId`, `userId`, `name`.
    - When handled.
    - Then a `Table` with matching `id=tableId`, `userId`, `name` exists in the repository.
    - Use `TableInMemoryRepository` (domain test fixture) to verify persistence.

7. **`TableVersionCreatedReadModelEventHandler should upsert version read model`**
    - Given a `TableVersionCreated` event with version data.
    - When handled.
    - Then `versionReadModelRepository.upsertVersion()` is called with correct arguments.
    - Use `VersionReadModelInMemoryRepository` (test fixture) to verify.

### Infrastructure Level — Database Integration Tests

#### `ExposedTableReadModelRepository` integration tests (new or updated)

8. **`should persist and retrieve table read model by userId`**
    - Call `create()` to insert a table read model entry into `table_read_model` table.
    - Call `findAllByUserId()`.
    - Assert returned entry matches (id, name, description).

9. **`should not return tables belonging to other users`**
    - Create entries for two different users.
    - Query by one userId.
    - Assert only that user's tables are returned.

10. **`should verify read model table is independent from domain table`**
    - Persist a domain `Table` via `ExposedTableRepository`.
    - Do NOT create corresponding read model entry.
    - Query `TableReadModelRepository.findAllByUserId()`.
    - Assert no results — proving read model does not read from domain table.

#### `VersionReadModelExposedRepository` integration tests (updated)

11. **`should retrieve versions from read model tables, not domain tables`**
    - Populate read model tables directly via `upsertVersion()`.
    - Query versions via `VersionReadModelExposedRepository`.
    - Assert data matches.

12. **`should return all versions for a table from read model`**
    - Same as existing test but data comes from read model tables.

13. **`should return current (latest) version from read model`**
    - Same as existing test but data comes from read model tables.

14. **`should handle hierarchical columns in read model`**
    - Insert nested column structures into read model tables.
    - Verify correct parent-child relationships are returned.

#### End-to-end event flow integration tests

15. **`should create Table aggregate via event when table is created in read model`**
    - Store table in read model via `tableReadModelRepository.create()`.
    - Call `tableCreatedDomainEventHandler.handle(TableCreated(...))`.
    - Verify domain `Table` exists in `tableRepository` with correct `id`, `userId`, `name`.
    - This tests the Read Model → Domain direction.

16. **`should project version into read model via event when version is created in domain`**
    - Set up a table (both read model + domain via the create flow).
    - Run `CreateTableVersionCommand` through `TableCommandHandler`.
    - Pass the resulting events to `tableVersionCreatedReadModelEventHandler.handle()`.
    - Query the version read model repository and verify version + columns exist.
    - This tests the Domain → Read Model direction.

17. **`should project multiple versions preserving history`**
    - Create table, add v1, pass events to handler.
    - Add v2, pass events to handler.
    - Verify both versions exist in read model with correct columns and order numbers.

### Infrastructure Level — HTTP Endpoint Tests

18. **`should create table and populate read model and domain via events`** (update existing)
    - POST `/api/tables` with name and description.
    - GET `/api/tables` should return the created table with description (from read model).
    - Verify the table also exists as a domain aggregate (enabling subsequent version creation).

19. **`should create version and read it from read model via events`** (update existing)
    - POST table creation, then POST version, then GET current version.
    - Data should come from read model, not domain model.

20. **`should not leak tables across users`** (existing test, should still pass)
    - Create tables for user A and user B.
    - GET `/api/tables` for user A should not include user B's tables.

All existing tests in `TableRoutesIntegrationTest.kt` should continue to pass. The in-memory test
repositories (`TableReadModelInMemoryRepository`, `VersionReadModelInMemoryRepository`) used in HTTP tests
are already decoupled from the domain — they just need their interface method signatures updated
(`addDescription` → `create`).

---

## Migration Strategy

Since we are working within a single bounded context and a single database, the migration can be done
in a single Flyway migration:

1. Create the four read model tables (`table_read_model`, `version_read_model`, `column_read_model`,
   `version_column_read_model`).
2. Copy existing data from domain tables into read model tables
   (including `description` from the domain `tables` table into `table_read_model`).
3. Copy existing version + column data from domain tables into the read model tables.
4. Drop `description` column from `tables` (domain table).

This ensures zero data loss and backward compatibility.

---

## Files to Create/Modify

### Domain module — new files

| File                                     | Action                                                                                  |
|------------------------------------------|-----------------------------------------------------------------------------------------|
| `domain/.../event/DomainEvent.kt`        | **Create** — marker interface for events                                                |
| `domain/.../event/DomainEventHandler.kt` | **Create** — handler interface                                                          |
| `domain/.../table/TableEvents.kt`        | **Create** — `TableCreated`, `TableVersionCreated` events                               |
| `domain/.../table/TableCommands.kt`      | **Create** — `CreateTableVersionCommand`, `TableCommandHandler` (version creation only) |

### Domain module — test files

| File                                          | Action                                                         |
|-----------------------------------------------|----------------------------------------------------------------|
| `domain/.../table/TableCommandHandlerTest.kt` | **Create** — unit tests for command handler and event emission |

### Infrastructure module — new files

| File                                                                                 | Action                                                                                     |
|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `infrastructure/.../table/event/TableCreatedDomainEventHandler.kt`                   | **Create** — consumes `TableCreated` → creates domain aggregate (Read Model → Domain)      |
| `infrastructure/.../table/event/TableVersionCreatedReadModelEventHandler.kt`         | **Create** — consumes `TableVersionCreated` → projects to read model (Domain → Read Model) |
| `infrastructure/.../table/readmodel/persistence/TableReadModelTables.kt`             | **Create** — Exposed table definitions for read model DB tables                            |
| `infrastructure/src/main/resources/db/migration/V8__table_read_model_decoupling.sql` | **Create** — read model tables + data migration                                            |

### Infrastructure module — modified files

| File                                                                      | Action                                                                           |
|---------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `infrastructure/.../table/persistence/Tables.kt`                          | **Modify** — remove `description` from `TablesTable`                             |
| `infrastructure/.../table/readmodel/TableReadModelRepository.kt`          | **Modify** — replace `addDescription` with `create`                              |
| `infrastructure/.../table/readmodel/ExposedTableReadModelRepository.kt`   | **Modify** — use read model tables, implement `create`                           |
| `infrastructure/.../table/readmodel/VersionReadModelRepository.kt`        | **Modify** — add `upsertVersion()` method                                        |
| `infrastructure/.../table/readmodel/VersionReadModelExposedRepository.kt` | **Modify** — use read model tables + implement `upsertVersion()`                 |
| `infrastructure/.../table/api/TableRoutes.kt`                             | **Modify** — CRUD-first table creation + direct event handler calls for versions |
| `infrastructure/.../table/dagger/TableRepositoryModule.kt`                | **Modify** — provide `TableCommandHandler`, event handlers                       |
| `infrastructure/.../StructifyAppComponent.kt`                             | **Modify** — expose `TableCommandHandler`, event handlers                        |
| `infrastructure/.../Application.kt`                                       | **Modify** — wire new dependencies                                               |

### Infrastructure module — deleted files

| File                                                               | Action                                  |
|--------------------------------------------------------------------|-----------------------------------------|
| `infrastructure/.../table/readmodel/TableReadModelSynchronizer.kt` | **Delete** — replaced by event handlers |

### Infrastructure module — test files

| File                                                                                   | Action                                                           |
|----------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `infrastructure/.../table/event/TableCreatedDomainEventHandlerTest.kt`                 | **Create** — unit tests                                          |
| `infrastructure/.../table/event/TableVersionCreatedReadModelEventHandlerTest.kt`       | **Create** — unit tests                                          |
| `infrastructure/.../table/readmodel/ExposedTableReadModelRepositoryIntegrationTest.kt` | **Create** — DB integration tests                                |
| `infrastructure/.../test/.../TableReadModelInMemoryRepository.kt`                      | **Modify** — update interface impl (`addDescription` → `create`) |
| `infrastructure/.../test/.../VersionReadModelInMemoryRepository.kt`                    | **Modify** — add `upsertVersion()` impl                          |
| `infrastructure/.../test/.../VersionReadModelExposedRepositoryIntegrationTest.kt`      | **Modify** — use read model tables for setup                     |
| `infrastructure/.../test/.../TestKtorApp.kt`                                           | **Modify** — wire new test dependencies                          |
