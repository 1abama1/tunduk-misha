# Build and deployment operations

CI runs Java 17, full Maven clean verify, Compose template validation, then a container build. All test failures fail the job; no production deployment or image publication is configured. The workflow itself has not been executed on GitHub during this local audit.

Docker builds with Maven 3.9.11 / Java 17 and runs a Corretto 17 Alpine runtime as an unprivileged app user. Its packaging stage retains the existing skipTests behavior because CI tests already run outside the build container. Never use a successful Docker package stage as evidence that tests passed. Base-image digests/container vulnerabilities have not been certified.

Compose waits for PostgreSQL pg_isready before starting the app. The container network remains db:5432/app:8080. Host PostgreSQL publishing changes from all interfaces to 127.0.0.1:5433 as a security default. Rollout plan: inventory any remote DB consumers first; migrate them to approved private networking or an explicit reviewed override before applying this Compose change. Keep postgres_data intact. Do not run down -v. APP_ADMIN_PASSWORD only initializes a missing administrator.

POSTGRES_DB is included in the default JDBC URL. A supplied SPRING_DATASOURCE_URL overrides it. Ensure the URL and POSTGRES_DB name agree; changing the variable does not rename an existing database in a persistent volume. Put secrets in untracked .env or the hosting secret mechanism; .dockerignore excludes .env, Git metadata and audit logs.

Validate with `docker compose config --quiet` locally without printing expanded secrets. Start with `docker compose up --build -d`. Read logs for Flyway validation, schema validation and application startup. PostgreSQL healthcheck is present; application DB-aware readiness, liveness and metrics are still absent. Configure TLS/reverse proxy and authentication throttling at the deployment boundary. Do not expose database ports publicly by default.

Local audit limitations: Docker CLI works but its daemon is unavailable, so image build and Compose runtime startup are unverified. A separate native PostgreSQL 17.6 instance provided integration evidence. Full clean verify passed on Java 17.0.12 and Java 21.0.12; the packaged Java 17 JAR was also started on test port 18089 and exercised over HTTP. Before production, execute the full container workflow, exercise API/database readiness and shutdown/restart, rehearse backup restore/upgrade, review CORS/frontend consumers, and complete dependency/container vulnerability scanning.
