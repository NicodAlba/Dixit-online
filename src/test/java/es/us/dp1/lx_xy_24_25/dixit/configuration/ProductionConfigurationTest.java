package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationTest {
    private MockEnvironment valid() {
        return new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://localhost/isolated")
            .withProperty("spring.datasource.username", "dixit_runtime")
            .withProperty("spring.datasource.password", "synthetic-local-test")
            .withProperty("spring.flyway.user", "dixit_migrator")
            .withProperty("spring.flyway.password", "synthetic-migration-test")
            .withProperty("dixit.database.runtime-role", "dixit_runtime")
            .withProperty("dp1.game.app.jwtSecret", Base64.getEncoder().encodeToString(new byte[64]))
            .withProperty("dixit.web.allowed-origins", "https://dixit.example")
            .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
            .withProperty("spring.sql.init.mode", "never");
    }
    @Test void acceptsCompleteConfiguration() { assertDoesNotThrow(() -> ProductionConfiguration.validate(valid())); }
    @Test void rejectsMissingEssentialConfigurationWithoutEchoingValues() {
        for (String property : new String[]{"spring.datasource.url", "spring.datasource.username",
                "spring.datasource.password", "spring.flyway.user", "spring.flyway.password",
                "dp1.game.app.jwtSecret", "dixit.web.allowed-origins"}) {
            var env = valid().withProperty(property, "");
            var error = assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(env));
            assertTrue(error.getMessage().contains(property));
            assertFalse(error.getMessage().contains("synthetic"));
        }
    }
    @Test void rejectsEphemeralOrShortSigningKeysAndUnsafeDatabaseDefaults() {
        for (String key : new String[]{"invalid", Base64.getEncoder().encodeToString(new byte[32])}) {
            assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("dp1.game.app.jwtSecret", key)));
        }
        for (String url : new String[]{"jdbc:h2:mem:test", "jdbc:postgresql://host/db?password=private"}) {
            assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.datasource.url", url)));
        }
        assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.flyway.user", "dixit_runtime")));
        assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.jpa.hibernate.ddl-auto", "create-drop")));
        assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.h2.console.enabled", "true")));
    }
    @Test void requiresVerifiedTlsForRemoteDatabaseAndValidDatabaseName() {
        for (String url : new String[]{"jdbc:postgresql://db.example/", "jdbc:postgresql://db.example/isolated?sslmode=disable"}) {
            assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.datasource.url", url)));
        }
        assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid()
            .withProperty("spring.datasource.url", "jdbc:postgresql://db.example/isolated")
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "disable")));
    }
    @Test void validatesSeparateMigrationEndpointAndItsTls() {
        assertDoesNotThrow(() -> ProductionConfiguration.validate(valid()
            .withProperty("spring.flyway.url", "jdbc:postgresql://migration.example/isolated")
            .withProperty("spring.flyway.jdbc-properties.sslmode", "verify-full")));
        for (String url : new String[]{"jdbc:h2:mem:wrong", "jdbc:postgresql://migration.example/db?password=private",
                "jdbc:postgresql://migration.example/db?sslmode=disable",
                "jdbc:postgresql://migration.example/db?sslfactory=org.postgresql.ssl.NonValidatingFactory",
                "jdbc:postgresql://migration.example/db?sslhostnameverifier=untrusted"}) {
            assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.flyway.url", url)));
        }
        assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid()
            .withProperty("spring.flyway.url", "jdbc:postgresql://migration.example/db")
            .withProperty("spring.flyway.jdbc-properties.sslmode", "disable")));
    }
    @Test void acceptsSupabaseSessionRolesAndRejectsUnverifiedTransactionPooling() {
        var env = valid().withProperty("spring.datasource.url", "jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:5432/postgres")
            .withProperty("spring.datasource.username", "dixit_runtime.project_ref")
            .withProperty("spring.flyway.user", "dixit_migrator.project_ref");
        assertDoesNotThrow(() -> ProductionConfiguration.validate(env));
        for (String url : new String[]{"jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres",
                "jdbc:postgresql://db.project.supabase.co:6543/postgres"}) {
            assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.datasource.url", url)));
            assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("spring.flyway.url", url)));
        }
    }
    @Test void rejectsWildcardsAndUntrustedInsecureOrigins() {
        for (String origin : new String[]{"*", "https://*.example", "http://dixit.example", "https://dixit.example/path", "https://dixit.example,"}) {
            assertThrows(IllegalStateException.class, () -> ProductionConfiguration.validate(valid().withProperty("dixit.web.allowed-origins", origin)), origin);
        }
        assertDoesNotThrow(() -> ProductionConfiguration.validate(valid().withProperty("dixit.web.allowed-origins", "http://127.0.0.1:8080")));
    }
}
