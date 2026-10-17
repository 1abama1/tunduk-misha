# Baseline — 2026-10-17

- Repository: `1abama1/tunduk-misha`; initial branch `main`, commit `8897c8ba48a45626fe5942ecee8a9ff9a0b8c9d5`; initially clean.
- Working branch: `audit/backend-hardening-20261017`.
- Java installed: Oracle 21.0.12; compilation target remains Java 17. Maven 3.9.11 is cached locally; `mvn` is absent from PATH.
- A separate JDK 17.0.12 was subsequently found at `C:\Program Files\Java\jdk-17`; final verification also ran on that target runtime.
- `mvnw.cmd -B clean verify` fails before Maven: indexing a null `Target` property in Windows PowerShell. Direct cached Maven compiles the original sources.
- First dependency resolution failed with `Permission denied: getsockopt`; download succeeded after sandbox escalation.
- **Incident:** the original `AuthServiceApplicationTests` has no isolated datasource configuration. It loaded the local `.env`, connected to `localhost:5432/kyrgyzCrm` (PostgreSQL 17.6) and applied the repository's existing V17 to public (previously V16). The run was interrupted. No rollback, clean, truncation or production-data experiment was performed. This is a test-isolation defect and an execution mistake; existing DB migration effects must be reviewed by its owner. See `baseline-maven.log`.
- A second baseline overrides the datasource to `127.0.0.1:1/audit_disabled` and disables Flyway, so the context test cannot touch the local DB. Result: **84 tests, 1 failure, 6 errors, 0 skipped**. The context failure is expected without a test DB; the other failures are pre-existing missing mock dependencies. See `baseline-isolated-tests.log`.
- Docker CLI 29.1.3 exists; Docker engine pipe is absent. Container build/runtime initially unavailable. `docker compose config --no-interpolate` parses; warns about obsolete `version` and inaccessible user Docker configuration.
- 20 controllers, 41 services, 15 repositories, 22 entities. Versioned migrations: V1, V4–V13, V16, V17, plus beforeMigrate callback. V2/V3/V14/V15 are absent.
- POM: Spring Boot 3.5.6, JJWT 0.11.5, POI 5.2.5, Lombok 1.18.32, compiler plugin 3.11.0. Exact resolved tree will be saved separately.
- Existing tests mostly assert mock-injected objects or MVC contexts are non-null; they are not evidence of business/security correctness.
- No production performance/load measurements or vulnerability scanner baseline exists.

Logs contain no intentionally emitted credentials. Do not run application-context tests against a developer or production database.
