package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit disposable CI database only. Never the managed application database. */
class BootstrapAdminIT {
    private Connection connect(String url) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", System.getenv("DIXIT_DB_USERNAME"));
        properties.setProperty("password", System.getenv("DIXIT_DB_PASSWORD"));
        properties.setProperty("sslmode", "disable");
        return DriverManager.getConnection(url, properties);
    }
    @Test void concurrentBootstrapsCreateExactlyOneBcryptAdministratorAndNeverOverwrite() throws Exception {
        String url = System.getenv("DIXIT_BOOTSTRAP_TEST_URL");
        assertEquals("jdbc:postgresql://127.0.0.1:5432/dixit_bootstrap", url, "Bootstrap IT requires its separate disposable CI DB");
        Flyway.configure().dataSource(url, System.getenv("DIXIT_MIGRATION_DB_USERNAME"), System.getenv("DIXIT_MIGRATION_DB_PASSWORD"))
            .schemas("dixit").defaultSchema("dixit").placeholders(Map.of("runtimeRole", "dixit_runtime")).load().migrate();
        String password = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> attempt = () -> {
                try (Connection connection = connect(url)) {
                    start.await(5, TimeUnit.SECONDS);
                    try { BootstrapAdmin.create(connection, "fixture_"+UUID.randomUUID(), password.toCharArray(), password.toCharArray()); return true; }
                    catch (IllegalStateException exists) { assertTrue(exists.getMessage().contains("already exists")); return false; }
                }
            };
            Future<Boolean> first = pool.submit(attempt), second = pool.submit(attempt);
            start.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            try (Connection connection = connect(url); Statement statement = connection.createStatement()) {
                String sql = "SELECT u.id,u.password FROM dixit.appusers u JOIN dixit.authorities a ON a.id=u.authority_id WHERE a.authority='ADMIN'";
                int id; String hash;
                try (ResultSet result = statement.executeQuery(sql)) {
                    assertTrue(result.next()); id = result.getInt(1); hash = result.getString(2);
                    assertFalse(result.next());
                    assertTrue(new BCryptPasswordEncoder().matches(password, hash));
                }
                assertThrows(IllegalStateException.class, () -> BootstrapAdmin.create(connection, "another_operator", password.toCharArray(), password.toCharArray()));
                try (ResultSet result = statement.executeQuery(sql)) {
                    assertTrue(result.next()); assertEquals(id, result.getInt(1)); assertEquals(hash, result.getString(2)); assertFalse(result.next());
                }
            }
        } finally { pool.shutdownNow(); }
    }
}
