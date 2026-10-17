# Tunduk rental CRM backend

Java 17 / Spring Boot 3.5.6 / PostgreSQL backend for clients, equipment, bookings, multi-tool rental contracts, trader sync and XLSX documents.

## Run locally

Use Java 17+ and Docker Compose. Copy `.env.example` to `.env`, replace every placeholder with independent random secrets, then run:

```sh
docker compose config --quiet
docker compose up --build -d
docker compose logs -f app
```

The API listens on port 8080. PostgreSQL is reachable from this host at `127.0.0.1:5433`; application containers use `db:5432`. Flyway runs before Hibernate schema validation. Never point a test process at a business database.

The bootstrap administrator is `admin@admin.admin`; its password comes from `APP_ADMIN_PASSWORD` only on initial creation. Changing that variable does not reset an existing administrator's password.

## Verify

```sh
bash mvnw -B clean verify
```

Windows: `./mvnw.cmd -B clean verify`. Context and concurrency tests automatically use a disposable PostgreSQL 17.6 Testcontainers instance; a working Docker daemon is required. An alternative explicitly isolated PostgreSQL server can be supplied through `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD`; the database name must end in `_test`. Every test JVM creates unique schemas. See [testing](docs/TESTING.md).

## Developer guide

- [Architecture](docs/ARCHITECTURE.md), [API and access matrix](docs/API.md), [security](docs/SECURITY.md).
- [Database and upgrade safety](docs/DATABASE.md), [deployment](docs/DEPLOYMENT.md), [offline synchronization](docs/SYNC_PROTOCOL.md).
- [Audit baseline](docs/audit/BASELINE.md), [risk register](docs/audit/RISK_REGISTER.md), [implementation report](docs/REFACTORING_REPORT.md).

`API_GUIDE.md` contains legacy frontend examples; current controller mappings are authoritative and its corrections section describes known differences. No deployment or push is performed by the audit workflow.
