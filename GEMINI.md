Project-specific development guidelines for Structify

Structify—the goal behind this app is to allow people to define a table with columns, for now just one dimension,
provide as much detailed information about the column as possible. This is because in the next steps the user will upload pdf files to an app.
App integrates with open ai rest api. App has a React ui allowing building tables.
Building table is basically defining columns and the description of what is this column so the LLM can find this information within provided documents.
App will be multitenant, meaning different users should not be able to access another user's data.

Audience: contributors familiar with Kotlin/JVM, Ktor, Gradle, Docker, and modern frontend tooling.

### Backend

The backend part is written in kotlin with gradle as a build tool, ktor, exposed for data access, flyway for database migrations.
There are two gradle modules. Domain module storing just the most important business logic. Infrastructure module using the domain module business model logic,
exposing it to interact with the world.
Back in service secured with oauth2.

#### Dependency injection

Use Dagger for dependency injection. Any code is structured in domain, feature slices including infrastructure. If there is a need to provide any http-client-specific functionality,
there should be a dedicated package containing a Dagger Module definition providing such implementation.

#### Persisting data

For the database access layer use Exposed. Do not use an Entity pattern from exposed. Use just tables to query and persist the data. Unfortunately, this will make you map from domain model
to a database model back and forth, but it's still better than entity from exposed. For database versioning migration version flyway migration files are stored under:
infrastructure/src/main/resources/db/migration.

#### Frontend

Front code is in the web/structify-ui dir. It's a react vite application. It is using keylock for security. It's using it's oauth2.

Overview

- Modules: domain (pure business logic) and infrastructure (Ktor server, persistence, DI). Frontend lives under web/structify-ui (React + Vite).
- Tech: Kotlin 2.1, Gradle Kotlin DSL, Ktor 3, Dagger 2 for DI, Exposed (DAO and plain tables) for DB access, Flyway for migrations, Postgres (with PostGIS extension), Keycloak for auth (OIDC + JWT),
  React/Vite for UI.
- Local services via Docker: docker/docker-compose.yaml spins up Postgres and Keycloak with pre-configured realm import.

Build and configuration
Backend (Gradle)

- Java toolchain: JDK 17+ recommended (Ktor 3 and Kotlin 2.1).
- Build all modules: ./gradlew clean build
- Run db migration ./gradlew flywayMigrate
- Build a single module: ./gradlew :domain:build or ./gradlew :infrastructure:build
- Run backend locally (development):
    - Ensure Postgres and Keycloak are up (see Docker section below).
    - From infrastructure module: ./gradlew :infrastructure:run
    - Ktor binds to port 8080 (configurable in infrastructure/src/main/resources/application.yaml)
    -

Configuration

- Application config: infrastructure/src/main/resources/application.yaml
    - Ktor module entry: io.structify.infrastructure.ApplicationKt.module
    - OAuth and JWT:
        - oauth.auth-url, oauth.redirect-url, oauth.access-token-url, oauth.client-id, oauth.secret, oauth.logout.url
        - jwt.jwk-url and jwt.issuer used by JwtPropertiesConfigFileProvider
    - DB settings for main and Keycloak schemas under db.*
- Environment overrides: all keys in application.yaml are strings like ${ENV_KEY}:default. You can override via env vars or JVM -D props. Example:
    - APP_STRUCTIFY_DB_URL, APP_STRUCTIFY_DB_USER, APP_STRUCTIFY_DB_PASS, APP_STRUCTIFY_DB_SCHEMA
    - APP_STRUCTIFY_KC_DB_URL, APP_STRUCTIFY_KC_DB_USER, APP_STRUCTIFY_KC_DB_PASS, APP_STRUCTIFY_KC_DB_SCHEMA
    - APP_STRUCTIFY_OAUTH_AUTH_URL, APP_STRUCTIFY_JWK_URL, APP_STRUCTIFY_ISSUER, etc.
- Flyway (infrastructure/build.gradle.kts): configured with getEnvOrProperty(...) fallback
    - Env vars: FLYWAY_DB_URL, FLYWAY_DB_USER, FLYWAY_DB_PASSWORD; defaults to jdbc:postgresql://localhost:5432/structify (user/pass structify)
    - Run migrations manually: ./gradlew :infrastructure:flywayMigrate

Local dependencies via Docker

- Compose file: docker/docker-compose.yaml
    - postgres: ghcr.io/baosystems/postgis:15-3.5, exposes 5432, initializes users/dbs via docker/postgres-init-script/init.sql
        - Creates users/databases:
            - structify/structify (DB user/password), database structify with PostGIS extension enabled
            - keycloak/keycloak (DB user/password), database keycloak
    - keycloak: quay.io/keycloak/keycloak:26.0.6, exposes 8282, starts with command: start-dev --import-realm
        - Imports docker/keycloak-imports/structify-realm.json
- Bring services up:
    - cd docker
    - docker compose up -d
    - Wait for Postgres and Keycloak health; Keycloak usually takes ~30–60s on first run.
- Stop services: docker compose down (data persists in named volume postgres_data)

Security & Auth

- JWT is enforced on secured routes using Application.installOAuthAuth() (infrastructure/src/main/kotlin/io/structify/infrastructure/security/OAuth.kt)
    - JWT verifier is built from jwkUrl and issuer; challenge returns 401.
    - For testing, infrastructure/src/test/kotlin/io/structify/infrastructure/test/MockJwtAuthenticationProvider.kt can inject a mocked JWTPrincipal in Ktor test context.

Persistence

- Exposed is used with DAO and table DSLs. Note: Although GEMINI.md discourages Exposed’s Entity pattern, current tests use LongEntity for integration test scaffolding; production code should prefer
  table DSL + manual mapping.
- Transaction orchestration: infrastructure/src/main/kotlin/io/structify/infrastructure/db/TransactionalRunner.kt
    - ExposedTransactionalRunner implements domain.db.TransactionalRunner, handling nested transactions using withSuspendTransaction when a Transaction is present.
- Test DB providers (infrastructure/src/test/...):
    - TestDatabaseConnectionProvider connects to jdbc:postgresql://localhost:5432/structify (structify/structify)
    - TestKeycloakDatabaseConnectionProvider connects to jdbc:postgresql://localhost:5432/keycloak (keycloak/keycloak)

Testing

- Frameworks: kotlin-test (JUnit), AssertJ, MockK, Awaitility (infra), Ktor server test host.
- Run all tests: ./gradlew test
- Run per module: ./gradlew :domain:test or ./gradlew :infrastructure:test
- Integration tests in infrastructure require Postgres (and sometimes Keycloak DB). Use Docker compose to provide these services before running tests.
    - The ExposedTransactionalRunnerIntegrationTest creates tables ad-hoc via SchemaUtils and relies on rollback to clean state.
- Using test fixtures: domain provides testFixtures (e.g., MockTransactionalRunner) for unit tests in infrastructure.
- test classes with runnable tests should be internal.
- test method names should follow patter naming with spaces and backticks. Example name of a test is: `should change current table version given new version of a table`.
- For assertions use assertj.
- Test methods should specify given when and then sections with comments.

How to add and run new tests

- Domain (unit tests only):
    - Place tests under domain/src/test/kotlin/...
    - Use kotlin-test, MockK, and testFixtures from domain as needed.
    - Example (verified):
        - File: domain/src/test/kotlin/io/structify/domain/SanityTest.kt
            - Content:
                - class SanityTest { @Test fun sanity() { assertEquals(4, 2 + 2) } }
        - Run: ./gradlew :domain:test
        - Expected: Test passes (we validated this flow).
- Infrastructure (integration tests with DB):
    - Ensure docker compose is running (see Local dependencies via Docker).
    - Use DatabaseIntegrationTest helpers to run code in rollbacked transactions.
    - For Ktor route tests, use io.ktor:ktor-server-test-host-jvm and consider MockJwtAuthenticationProvider to bypass real JWT.

Frontend (web/structify-ui)

- Node/Yarn: Use Node 18+ (prefer LTS) and npm or yarn.
- Install deps: cd web/structify-ui && npm ci
- Run dev server: npm run dev (defaults to Vite dev server; configure proxies if calling backend)
- Build: npm run build; preview: npm run preview
- Keycloak: Frontend is intended to use Keycloak for authentication,
- Material UI is a web components library.

Coding conventions & structure

- DI with Dagger 2 (infrastructure):
    - Modules under infrastructure/src/main/kotlin/.../dagger. Prefer constructor injection; provide interfaces in modules.
- Domain vs Infrastructure boundaries:
    - Domain should be pure (no IO), Kotlin-only; infrastructure wires everything to external systems (DB, HTTP, security).
- Exposed usage:
    - Prefer table DSL; avoid Entity pattern in production code. Map to domain types explicitly.
    - Use ExposedTransactionalRunner for all DB access to support nesting and coroutine contexts.
- Configuration:
    - All externalized config is overridable via env or -D; keep defaults matching docker-compose.
- Logging: Logback is present; configure appenders in infrastructure/resources if needed.

Debugging tips

- DB: Connect to localhost:5432 with psql or a GUI using structify/structify and keycloak/keycloak.
- JWT issues: Verify jwt.issuer and jwk-url match Keycloak base http://localhost:8282/realms/structify.
- Flyway: If schema mismatch, run flywayMigrate; baselineOnMigrate=true helps align fresh DBs.
- Ktor routing: Secure routes require Authorization: Bearer <token>; for tests, use MockJwtAuthenticationProvider.

Example end-to-end local run

1) Start infra: cd docker && docker compose up -d
2) Apply DB migrations (optional during dev): ./gradlew :infrastructure:flywayMigrate
3) Run backend: ./gradlew :infrastructure:run (listens on 8080)
4) Run frontend: cd web/structify-ui && npm ci && npm run dev (configure CORS/proxy as needed)

Example test run (validated)

- Unit test (domain module): ./gradlew :domain:test passed using a sample test file. To add your own, place under domain/src/test/kotlin and rerun the command.

Maintenance notes

- Docker volume postgres_data persists DBs; use docker volume rm structify_postgres_data to reset if needed.
- If ports 5432 or 8282 are in use, adjust docker-compose.yaml and application.yaml accordingly.
- Remember that every file should end with a single empty line.
