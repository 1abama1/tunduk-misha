package org.misha.authservice;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MigrationSafetyTest extends PostgresTestSupport {
    @Value("${spring.datasource.url}") String url;
    @Value("${spring.datasource.username}") String user;
    @Value("${spring.datasource.password}") String password;

    @Test void emptyDatabaseMigratesAndSecondRunIsNoOp() {
        var f = isolated();
        assertTrue(f.flyway(null).migrate().migrationsExecuted >= 13);
        f.flyway(null).validate();
        assertEquals(0, f.flyway(null).migrate().migrationsExecuted);
    }

    @Test void upgradingV16BackfillsEveryToolAndPreservesExistingRows() {
        var f = isolated(); f.flyway("16").migrate();
        UUID category = UUID.randomUUID(), template = UUID.randomUUID();
        f.jdbc.update("INSERT INTO tool_categories(id,name) VALUES (?,?)", category, "historic");
        f.jdbc.update("INSERT INTO tool_templates(id,name,category_id,created_at) VALUES (?,?,?,NOW())", template, "historic", category);
        long client = f.jdbc.queryForObject("INSERT INTO clients(full_name) VALUES ('historic client') RETURNING id", Long.class);
        long document = f.jdbc.queryForObject("INSERT INTO rental_documents(contract_number,client_id,created_at,offline_id) VALUES ('historic',?,NOW(),'offline-history') RETURNING id", Long.class, client);
        long a = f.jdbc.queryForObject("INSERT INTO tool_instances(template_id,contract_id,created_at) VALUES (?,?,NOW()) RETURNING id", Long.class, template, document);
        long b = f.jdbc.queryForObject("INSERT INTO tool_instances(template_id,contract_id,created_at) VALUES (?,?,NOW()) RETURNING id", Long.class, template, document);
        f.jdbc.update("UPDATE rental_documents SET tool_id=? WHERE id=?", a, document);
        f.flyway(null).migrate();
        assertEquals(2, f.jdbc.queryForObject("SELECT count(*) FROM rental_document_tools WHERE document_id=? AND tool_id IN (?,?)", Integer.class, document, a, b));
        assertEquals("offline-history", f.jdbc.queryForObject("SELECT offline_id FROM rental_documents WHERE id=?", String.class, document));
        assertEquals(2, f.jdbc.queryForObject("SELECT count(*) FROM tool_instances WHERE contract_id=?", Integer.class, document));
    }

    @Test void populatedLegacyConversionIsBlockedWithoutDeletingData() {
        var f = isolated(); f.flyway("1").migrate();
        f.jdbc.update("INSERT INTO tool_categories(name) VALUES ('must survive')");
        assertThrows(FlywayException.class, () -> f.flyway(null).migrate());
        assertEquals(1, f.jdbc.queryForObject("SELECT count(*) FROM tool_categories", Integer.class));
    }

    @Test void checksumMismatchIsRejected() {
        var f = isolated(); f.flyway(null).migrate();
        f.jdbc.update("UPDATE flyway_schema_history SET checksum=0 WHERE version='17'");
        assertThrows(FlywayException.class, () -> f.flyway(null).validate());
    }

    private Fixture isolated() {
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        var source = new DriverManagerDataSource(url.replaceAll("\\?.*$", "") + "?currentSchema=" + schema, user, password);
        new JdbcTemplate(new DriverManagerDataSource(url, user, password)).execute("CREATE SCHEMA " + schema);
        return new Fixture(source, new JdbcTemplate(source), schema);
    }

    private record Fixture(DriverManagerDataSource source, JdbcTemplate jdbc, String schema) {
        Flyway flyway(String target) {
            var config = Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).baselineOnMigrate(false);
            if (target != null) config.target(target);
            return config.load();
        }
    }
}
