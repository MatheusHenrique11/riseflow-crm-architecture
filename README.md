# RiseFlow CRM

RiseFlow CRM by RiseCode Studio unifies CRM, growth marketing automation, and transactional commerce capabilities in a multi-tenant SaaS architecture.

## Stack

- Java 21, Spring Boot 3.2.x, Spring Modulith, Maven
- PostgreSQL 16, Redis 7, RabbitMQ 3.12, Keycloak 23
- Flyway, MapStruct, Lombok, Springdoc OpenAPI
- JUnit 5, Mockito, AssertJ, Testcontainers

## Setup

```bash
docker compose up -d
mvn test
mvn spring-boot:run -pl riseflow-api -am
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

## Modules

- `riseflow-core`: shared kernel, base entities, repositories, events, exceptions, tenant context.
- `riseflow-module-tenant`: tenant CRUD and public schema master data.
- `riseflow-module-accounts`: tenant-aware account CRUD.
- `riseflow-module-deals`: pipeline and deal tracking — pipeline CRUD with default-pipeline logic, ordered stages with probability, deal lifecycle (create, update, move between stages), status derivation (OPEN/WON/LOST from stage probability), deal filtering by pipeline/stage/responsible/status, and custom field validation reusing accounts definitions.
- `riseflow-module-marketing`: campaigns with status lifecycle, email templates, automation rules triggered by CRM events, and multi-step sequences with enrollment/execution tracking; email and WhatsApp senders are logging stubs pending real provider integration.
- `riseflow-module-commercial`: invoices and proposals with line items and status lifecycles, commission calculation, a scheduled overdue-invoice checker, and a `PaymentGateway` abstraction with a logging stub pending a real provider (e.g. Stripe) integration.
- `riseflow-api`: Spring Boot application aggregating all modules.

## Testing

Run all unit and integration tests:

```bash
mvn test
```

Integration tests use Testcontainers for PostgreSQL where Docker is available.
