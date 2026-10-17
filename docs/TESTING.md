# Reproducible verification

Run `bash mvnw -B clean verify` (Windows `./mvnw.cmd -B clean verify`) on Java 17+ with Docker available. The complete suite includes the existing context smoke test; no test was deleted or disabled to make it pass. PostgresTestSupport creates a PostgreSQL 17.6 Testcontainer unless an explicit test database is supplied.

For a separately provisioned native/CI test server:

```powershell
$env:TEST_DATABASE_URL='jdbc:postgresql://127.0.0.1:55439/tunduk_audit_test'
$env:TEST_DATABASE_USERNAME='audit_test'
# Set TEST_DATABASE_PASSWORD through your test environment if required.
.\mvnw.cmd -B clean verify
```

The database name must end in `_test`; URLs with query parameters are rejected. Each JVM creates a random audit schema and migration tests create additional random schemas. Dynamic datasource, Flyway, JWT and bootstrap properties override .env/application settings. The test server remains responsible for preventing its credentials from reaching a business database. Schemas are intentionally retained for failure diagnosis; remove them only in a disposable test environment. Do not supply production secrets.

The local audit used a new native PostgreSQL cluster under sibling `audit/postgres-isolated-20261017` listening only on 127.0.0.1:55439. It used no business-database password. The cluster can be stopped with its matching pg_ctl; do not stop any other PostgreSQL instance. The initial sandbox initdb attempt created an unused separate directory, which is not a running server.

Behavior coverage added:

- One winner for concurrent rental, duplicate offline creation, complete multi-tool close/restore, busy restore rollback.
- Overlapping booking races and adjacent periods; distinct usable-instance availability.
- Revision conflict batch rollback; update and close within one batch compare the initial revision.
- Concurrent receipt replay, payload conflict and receipt/business rollback followed by retry.
- Concurrent refresh, replay revocation commit, stored expiry/owner/JTI checks.
- Soft-deleted contracts disappear from full snapshots while tool history persists; historical clients remain protected.
- Empty migrations, V16 upgrade/backfill, rejected populated legacy conversion, checksum rejection.
- Actual SecurityFilterChain roles/routes and bearer failures; JWT type/expiry/signature/subject.
- Actual XLSX template roundtrip, all ten tools on continuation sheets, text/formula handling.
- Input/parameter errors, API-key ownership, trader sync failure boundaries and upload response headers.

The race harness uses latches and independent service transactions, with bounded futures; no Thread.sleep, H2 or mocked PostgreSQL semantics. Two contenders per race are regression protection, not a production load test. Old mock-only smoke tests remain and should gradually become meaningful behavior tests. Coverage percentages and the requested 90% critical-module target have not been measured.

See the [baseline](audit/BASELINE.md) and [report](REFACTORING_REPORT.md) for actual counts/results. Local execution logs remain in ignored docs/audit/*.log; `DEPENDENCIES.txt`, migration hashes and reports are reviewable artifacts. `python scripts/audit_inventory.py` regenerates the HTTP/transaction inventory without starting Spring.
