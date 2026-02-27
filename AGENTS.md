# Structify
Structify—the goal behind this app is to allow people to define a table with columns, for now just one dimensional.
Provide as much detailed information about the column as possible describing what does the column store. This is because in the next steps the user will upload PDF files to an app from which this data
will be extracted using AI. App integrates with OpenAI rest api. App has a React ui allowing building tables. Building table is basically defining columns and the description of what is this column so
the LLM can find this information within provided documents. App is multitenant, meaning different users should not be able to access another user's data.

# Security
App is secured with oauth2 with keycloak. Client requests are authenticated using JWT tokens.

# Backend
The backend part is written in kotlin with gradle as a build tool, ktor, exposed for data access, flyway for database migrations.
The backend is implemented using hexagonal (ports and adapters) architecture.

There are two gradle modules:
- `./domain` gradle module storing just the most important business logic defining infrastrucutral interfaces to be implemented-ports. The `domain` module in fact is just one bounded context in
  Domain Driven Design sense. The `domain` module packages are split into subdomains. Subdomains are under `domain/src/main/kotlin/io/structify/domain` package.
- `./infrastructure` gradle module using the domain module business model logic, exposing it to interact with the world. Implementing ports and adapters.
  Backend services are secured with oauth2.

## Subdomains & Event driven
Each subdomain should be as much cohesive as possible. Each subdomain should contain possibly all functionalities relating its domain. Each subdomain is not allowed to modify any of other domain
aggregates or entities, both directly and indirectly via other domain services, command handlers. However they can use repositories to read other domains state. If action of one subdomain must invoked
a change in another subdomain, originating subdomain should then emit an event that will be consumed by another subdomain e.g.: one subdomain command handler returns as a result an `event` that then will
be consumed by another subdomain event handler.

## CQRS
Backend is currently implemented with simplified CQRS pattern. Meaning domain models (aggregates, entities, value objects) can be different from read models used to display data for the end user reading purposes.
Domain models should only hold necessary data required for processing purposes, domain constrain validations. Read models on the other hand are complete data set describing a domain model for user
read purposes. However read models can read data straight from the domain model database tables.
It is very important to differentiate what data belongs to domain model and what is purely read model data.
Data that existing only for user read purposes, not participating in any domain rules processing, should be stored apart from domain model and treated as simple CRUD data.
CRUD data is not required to have corresponding domain model as it would simple not contain any code apart from data structure definitions.
Domain model data is independent of read model data.

## Command handlers
Commands and command handlers are responsible of handling crucial use cases resulting in domain model state change. They reflect a single use case e.g.:
command to extract data from the given text and table will require to read the current table definition, extract data from text according to this definition and then create a row to populate the table.

## Dependency injection
Use Dagger for dependency injection. All code is structured in feature slices. If there is a need to provide any http-client-specific functionality,
there should be a dedicated package containing a Dagger Module definition providing such implementation.

## Persisting data
For the database access layer use Exposed. Do not use an Entity pattern from exposed. Use just tables to query and persist the data. Unfortunately, this will make you map from a domain model
to a database model back and forth, but it's still better than entity from exposed. For database versioning migration version flyway migration files are stored under:
infrastructure/src/main/resources/db/migration.

## Testing
Try not to use mocking library. Use it only when it is absolutely necessary. Use mocking library hen creating a custom mock implemention would unnecessarily bloat the implementation or reduce readability.
We favour mock implemention of interfaces instead. Obviously you can use mocking library when it is impossible to use mock implementation.
- Unit tests:
    - Domain module code should aim for 100% code coverage. Domain code should be organized the way it allows extensive unit testing.
    - Do not use mocks to imitate aggregate roots or entities. Code should be organized in testable, concise way allowing testing with real aggregate roots and entities. Just create the with production constructors or factory functions.

- Integration tests:
    - database integration tests:
        - they should extend @infrastructure/src/test/kotlin/io/structify/infrastructure/test/DatabaseIntegrationTest.kt allowing to execute underlying db queries against real database.
    - http endpoint tests:
        - They should use test ktor app setup @infrastructure/src/test/kotlin/io/structify/infrastructure/test/TestKtorApp.kt

---

# Frontend
Located under web/loveable. It is react ui generated with AI tool called loveable.
