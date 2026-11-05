package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

/** Only an explicit isolated loopback PostgreSQL fixture, never managed data. */
class ResetAdminPasswordIT {
    @Test void changesOnlyTheExactAdminPasswordAndRejectsPlayerOrMismatchedIdentity() throws Exception {
        String url = System.getenv("DIXIT_TEST_POSTGRES_URL");
        assertNotNull(url); assertTrue(url.startsWith("jdbc:postgresql://127.0.0.1:"));
        assertEquals(url, System.getenv("DIXIT_DB_URL"));
        Flyway.configure().dataSource(url, System.getenv("DIXIT_MIGRATION_DB_USERNAME"), System.getenv("DIXIT_MIGRATION_DB_PASSWORD"))
            .schemas("dixit").defaultSchema("dixit").placeholders(Map.of("runtimeRole", "dixit_runtime")).load().migrate();
        String username = "recovery_" + UUID.randomUUID().toString().substring(0,16);
        char[] fresh = UUID.randomUUID().toString().toCharArray();
        var bcrypt = new BCryptPasswordEncoder(); String previous = bcrypt.encode("Synthetic original fixture");
        try (var connection = DriverManager.getConnection(url, System.getenv("DIXIT_DB_USERNAME"), System.getenv("DIXIT_DB_PASSWORD"))) {
            int admin, player;
            try (var insert = connection.prepareStatement("INSERT INTO dixit.appusers(id,username,password,authority_id) SELECT nextval('dixit.entity_sequence'),?,?,id FROM dixit.authorities WHERE authority=? RETURNING id")) {
                insert.setString(1, username); insert.setString(2, previous); insert.setString(3, "ADMIN");
                try (var row = insert.executeQuery()) { row.next(); admin = row.getInt(1); }
                insert.setString(1, username + "_player"); insert.setString(3, "PLAYER");
                try (var row = insert.executeQuery()) { row.next(); player = row.getInt(1); }
            }
            try {
                long sequence;
                try (var query = connection.createStatement(); var row = query.executeQuery("SELECT last_value FROM dixit.entity_sequence")) { row.next(); sequence = row.getLong(1); }
                assertThrows(IllegalStateException.class, () -> ResetAdminPassword.reset(connection, player, username + "_player", fresh, fresh));
                assertThrows(IllegalStateException.class, () -> ResetAdminPassword.reset(connection, admin, username + "_wrong", fresh, fresh));
                ResetAdminPassword.reset(connection, admin, username, fresh, fresh);
                try (var query = connection.prepareStatement("SELECT u.username,u.password,a.authority FROM dixit.appusers u JOIN dixit.authorities a ON a.id=u.authority_id WHERE u.id=?")) {
                    query.setInt(1, admin);
                    try (var row = query.executeQuery()) {
                        assertTrue(row.next()); assertEquals(username,row.getString(1)); assertTrue(bcrypt.matches(java.nio.CharBuffer.wrap(fresh),row.getString(2)));
                        assertFalse(bcrypt.matches("Synthetic original fixture",row.getString(2))); assertEquals("ADMIN",row.getString(3));
                    }
                    query.setInt(1, player);
                    try (var row = query.executeQuery()) { assertTrue(row.next()); assertEquals(previous,row.getString(2)); assertEquals("PLAYER",row.getString(3)); }
                }
                try (var query = connection.createStatement(); var row = query.executeQuery("SELECT last_value FROM dixit.entity_sequence")) { row.next(); assertEquals(sequence,row.getLong(1)); }
            } finally {
                try (var delete = connection.prepareStatement("DELETE FROM dixit.appusers WHERE id IN (?,?)")) { delete.setInt(1,admin);delete.setInt(2,player);delete.executeUpdate(); }
            }
        }
    }
}
