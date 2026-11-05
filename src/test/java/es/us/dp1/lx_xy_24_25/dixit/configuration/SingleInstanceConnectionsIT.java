package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit PostgreSQL-only tests. No application rows, credentials or secrets in output. */
class SingleInstanceConnectionsIT {
    private String identity() { return "dixit.runtime." + UUID.randomUUID(); }
    private Connection open() throws SQLException {
        return DriverManager.getConnection(System.getenv("DIXIT_DB_URL"),
            System.getenv("DIXIT_DB_USERNAME"), System.getenv("DIXIT_DB_PASSWORD"));
    }
    private Connection claim(String identity) throws SQLException {
        Connection connection = open();
        try {
            connection.createStatement().execute(SingleInstanceConnections.initializationSql(identity));
            return connection;
        } catch (SQLException error) { connection.close(); throw error; }
    }
    private void rejected(String identity) {
        SQLException error = assertThrows(SQLException.class, () -> { try (var ignored = claim(identity)) { } });
        assertEquals("55000", error.getSQLState());
    }
    @Test void everyPhysicalSessionFencesOtherProcessesUntilTheLastOneCloses() throws Exception {
        String first = identity(), replacement = identity();
        try (var a = claim(first)) {
            try (var b = claim(first)) { rejected(replacement); }
            rejected(replacement);
        }
        try (var winner = claim(replacement)) { rejected(first); }
    }
    @Test void concurrentClaimantsHaveExactlyOneWinner() throws Exception {
        var start = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var successes = new java.util.concurrent.atomic.AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Boolean> contender = () -> {
                start.await();
                try (var connection = claim(identity())) {
                    successes.incrementAndGet(); finish.await(10, TimeUnit.SECONDS); return true;
                } catch (SQLException conflict) { assertEquals("55000", conflict.getSQLState()); return false; }
            };
            var a = executor.submit(contender); var b = executor.submit(contender); start.countDown();
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (!a.isDone() && !b.isDone() && System.nanoTime() < deadline) Thread.sleep(10);
                assertTrue(a.isDone() || b.isDone(), "Loser must fail while the winner still holds its session");
                assertEquals(1, successes.get());
            } finally { finish.countDown(); }
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        }
    }
    @Test void lostConnectionCannotReconnectAgainstTheReplacement() throws Exception {
        String original = identity(), replacement = identity();
        try (var old = claim(original); var terminator = open()) {
            int pid;
            try (var result = old.createStatement().executeQuery("select pg_backend_pid()")) { result.next(); pid = result.getInt(1); }
            try (var statement = terminator.prepareStatement("select pg_terminate_backend(?, 5000)")) {
                statement.setInt(1, pid); try (var result = statement.executeQuery()) { result.next(); assertTrue(result.getBoolean(1)); }
            }
            assertThrows(SQLException.class, () -> old.createStatement().execute("select 1"));
            try (var winner = claim(replacement)) { rejected(original); }
        }
    }
    @Test void hikariKeepsTheGuardAfterLogicalReturnAndReleasesItOnPoolClose() throws Exception {
        String first = identity(), replacement = identity();
        try (var pool = new HikariDataSource()) {
            pool.setJdbcUrl(System.getenv("DIXIT_DB_URL"));
            pool.setUsername(System.getenv("DIXIT_DB_USERNAME")); pool.setPassword(System.getenv("DIXIT_DB_PASSWORD"));
            pool.setMaximumPoolSize(2); pool.setMinimumIdle(0);
            SingleInstanceConnections.configure(pool, first);
            assertEquals(1, pool.getMinimumIdle());
            try (var a = pool.getConnection(); var b = pool.getConnection()) { rejected(replacement); }
            rejected(replacement);
        }
        try (var winner = claim(replacement)) { rejected(first); }
    }
}
