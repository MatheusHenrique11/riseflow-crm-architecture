# RiseFlow CRM

RiseFlow CRM by RiseCode Studio unifies CRM, growth marketing automation, and transactional commerce capabilities in a multi-tenant SaaS architecture.

## Stack

- Java 21, Spring Boot 3.2.x, Spring Modulith, Maven
- PostgreSQL 16, Keycloak 23
- Flyway, Lombok, Springdoc OpenAPI, Spring Boot Actuator
- JUnit 5, Mockito, AssertJ, Testcontainers

## Setup

```bash
docker compose up -d
mvn test
mvn spring-boot:run -pl riseflow-api -am
```

Copy `.env.example` to `.env` and adjust values if you need non-default credentials,
CORS origins, or to point the app at infrastructure that isn't running via
`docker compose` (all settings have safe local-dev defaults, so this step is optional).

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

Health checks are available at:

```text
http://localhost:8080/actuator/health
```

### Running the app in a container

A multi-stage `Dockerfile` builds and packages `riseflow-api`:

```bash
docker build -t riseflow-api .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/riseflow \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://host.docker.internal:8081/realms/riseflow \
  riseflow-api
```

The `issuer-uri` must match the host clients use to request tokens from Keycloak
(the JWT's `iss` claim is validated against it), so adjust it for your environment.

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

A GitHub Actions workflow (`.github/workflows/ci.yml`) runs the full build and test
suite on every push and pull request to `main`.
