# Security

App is secured with oauth2 with keycloak. Client requests are authenticated using JWT tokens.

# Backend

The backend part is written in kotlin with Gradle as a build tool, ktor as application framework, exposed for data
access, flyway for database migrations.
The backend is implemented using hexagonal (ports and adapters) architecture and follows CQRS pattern.

There are two Gradle modules:

- `./domain` Gradle module storing just the most important business logic defining infrastrucutral interfaces to be
  implemented-ports.
  The `domain` module in fact is just one bounded context of structify in Domain Driven Design sense. The `domain`
  module
  packages are split into subdomains.
- `./infrastructure` Gradle module using the domain module business model logic, exposing it to interact with the world.
  Implementing ports and adapters.

## CQRS

Backend is implemented with a CQRS pattern.
Meaning domain models (aggregates, entities, value objects) can be different from read models used to display data for
the end user reading purposes.
Domain models should only hold necessary data required for request, command, event processing purposes, domain constrain
validations.
Read models, on the other hand, are complete data set view, picturing the domain model for user read purposes.
Read model must not read any data from a domain model.
It is crucial to differentiate what data belongs to a domain model and what is purely read model data.
Data existing only for user read purposes, not participating in any domain rules processing must be stored apart from a
domain model and treated as simple CRUD data.
CRUD data is not required to have a corresponding domain model as it does not protect business invariants.
Domain model data must be independent of read model data.

### Read model and domain model synchronization

It is important to identify uniquely identifiable domain model objects and their corresponding read model objects.
When the problem to be solved contains both CRUD and processing characteristics, it can be solved by:

1. CRUD-read model emitting events that are consumed by domain model creating an aggregate and entities.
2. Domain model emitting events that are consumed by read model to update its state.
   For now, as there is only one bounded context with subdomain packages, it is fine to execute domain logic and consume
   the domain event in a read model within the same transaction.

## Subdomains & Event driven

Each subdomain should be as much cohesive as possible.
Each subdomain should contain possibly all functionalities relating to its domain.
Each subdomain is not allowed to modify any other domain state, their aggregates or entities,
both directly and indirectly via other domain services, command handlers, etc.
However, they can use repositories to read other domains state. If action of one subdomain must invoke
a change in another subdomain, originating subdomain operation or command handling must result in an event
that will be consumed by another subdomain e.g.: one subdomain command handler returns as a result an `event` that
then will be consumed by another subdomain event handler.

## Command handlers

Commands and command handlers are responsible for handling crucial use cases resulting in domain model state change.
They reflect a single use case e.g.: command to extract data from the given text to a table will require to read the
current table definition, extract data from the text according to this definition and then create a row to populate the
table.

## Dependency injection

Use Dagger for dependency injection. All code is structured in feature slices. If there is a need to provide any
http-client-specific functionality,
there should be a dedicated package containing a Dagger Module definition providing such implementation.

## Persisting data

For the database access layer use Exposed.
Do not use an Entity pattern from exposed.
Use just tables to query and persist the data.
Unfortunately, this will make you map from a domain model to a database model back and forth.
For database versioning migration version flyway migration files are stored under:
infrastructure/src/main/resources/db/migration.

## Testing

Try not to use a mocking library. Use it only when it is necessary.
Use mocking library hen creating a custom mock implemention would unnecessarily bloat the implementation or reduce
readability.
We favor mock implemention of interfaces instead. You can use a mocking library when it is impossible to use a a mock
implementation.

- Unit tests:
    - Domain module code should aim for 100% code coverage. Domain code should be organized the way it allows extensive
      unit testing.
    - Do not use mocks to imitate aggregate roots or entities. Code should be organized in a testable,
      concise way allowing testing with real aggregate roots and entities. Create them with production constructors or
      factory functions.

- Integration tests:
    - database integration tests:
        - they should extend @infrastructure/src/test/kotlin/io/structify/infrastructure/test/DatabaseIntegrationTest.kt
          allowing to execute underlying db queries against a real database.
    - http endpoint tests:
        - They should use test ktor app setup
          @infrastructure/src/test/kotlin/io/structify/infrastructure/test/TestKtorApp.kt
