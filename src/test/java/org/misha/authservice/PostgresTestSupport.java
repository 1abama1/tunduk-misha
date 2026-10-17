package org.misha.authservice;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.util.UUID;

/** All context tests use a container or an explicit test database and a unique schema. */
public abstract class PostgresTestSupport {
    private static final String SCHEMA = "audit_" + UUID.randomUUID().toString().replace("-", "");
    private static final PostgreSQLContainer<?> CONTAINER;
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        String configured = System.getenv("TEST_DATABASE_URL");
        if (configured == null || configured.isBlank()) {
            CONTAINER = new PostgreSQLContainer<>("postgres:17.6-alpine");
            CONTAINER.start();
            URL = CONTAINER.getJdbcUrl();
            USER = CONTAINER.getUsername();
            PASSWORD = CONTAINER.getPassword();
        } else {
            // Prevent accidental reuse of application.properties / .env or a business database.
            if (!configured.matches("jdbc:postgresql://[^/?]+/[a-zA-Z0-9_]*_test")) {
                throw new IllegalArgumentException("TEST_DATABASE_URL must name a dedicated database ending in _test (no URL parameters)");
            }
            CONTAINER = null;
            URL = configured;
            USER = System.getenv().getOrDefault("TEST_DATABASE_USERNAME", "audit_test");
            PASSWORD = System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "");
        }
        try (var connection = DriverManager.getConnection(URL, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException("Cannot create isolated test schema", ex);
        }
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> URL + "?currentSchema=" + SCHEMA);
        properties.add("spring.datasource.username", () -> USER);
        properties.add("spring.datasource.password", () -> PASSWORD);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.flyway.baseline-on-migrate", () -> false);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("jwt.secret", () -> "test-only-signing-key-32-bytes-minimum-never-use-in-production");
        properties.add("app.admin.password", () -> "test-only-admin-password");
        properties.add("spring.jpa.open-in-view", () -> false);
    }
}
