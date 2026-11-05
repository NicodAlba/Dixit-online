package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.*;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Properties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Owner-run offline recovery only: no endpoint, default password or password arguments. */
public final class ResetAdminPassword {
    private ResetAdminPassword() { }
    public static void main(String[] args) {
        char[] password = null, confirmation = null;
        try {
            var console = System.console();
            if (args.length != 0 || console == null) throw new IllegalArgumentException("Use an interactive terminal with hidden password input.");
            String username = console.readLine("Existing administrator username: ");
            int id = Integer.parseInt(console.readLine("Existing administrator account ID: "));
            password = console.readPassword("New administrator password (12+ characters): ");
            confirmation = console.readPassword("Confirm new password: ");
            var env = System.getenv();
            String url = env.getOrDefault("DIXIT_DB_URL", "");
            String ssl = env.getOrDefault("DIXIT_DB_SSL_MODE", "verify-full");
            ProductionConfiguration.validateDatabaseUrl(url, ssl);
            var properties = new Properties();
            for (String name : new String[]{"DIXIT_DB_USERNAME", "DIXIT_DB_PASSWORD"})
                if (env.getOrDefault(name, "").isBlank()) throw new IllegalArgumentException("Missing backend database configuration.");
            properties.setProperty("user", env.get("DIXIT_DB_USERNAME"));
            properties.setProperty("password", env.get("DIXIT_DB_PASSWORD"));
            properties.setProperty("sslmode", ssl);
            properties.setProperty("sslrootcert", env.getOrDefault("DIXIT_DB_SSL_ROOT_CERT", System.getProperty("user.home") + "/.postgresql/root.crt"));
            properties.setProperty("connectTimeout", "10");
            try (var connection = DriverManager.getConnection(url, properties)) { reset(connection, id, username, password, confirmation); }
            console.printf("Administrator password updated. Account, role and history preserved.%n");
        } catch (SQLException error) {
            System.err.println("Administrator recovery failed (SQL state " + error.getSQLState() + "); no credentials printed."); System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException error) {
            System.err.println(error.getMessage()); System.exit(1);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
    static void reset(Connection connection, int id, String username, char[] password, char[] confirmation) throws SQLException {
        BootstrapAdmin.validateInput(username, password, confirmation);
        if (id <= 0 || !connection.getAutoCommit()) throw new IllegalArgumentException("Recovery requires a positive account ID and a dedicated connection.");
        connection.setAutoCommit(false);
        try {
            try (var check = connection.createStatement(); var result = check.executeQuery(
                    "SELECT rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls "
                    + "OR has_schema_privilege(current_user,'dixit','CREATE') OR has_database_privilege(current_user,current_database(),'CREATE') FROM pg_roles WHERE rolname=current_user")) {
                if (!result.next() || result.getBoolean(1)) throw new IllegalStateException("Use the restricted runtime role for account recovery.");
            }
            try (var select = connection.prepareStatement("SELECT u.id FROM dixit.appusers u JOIN dixit.authorities a ON a.id=u.authority_id WHERE u.id=? AND u.username=? AND a.authority='ADMIN' FOR UPDATE OF u")) {
                select.setInt(1, id); select.setString(2, username);
                try (var result = select.executeQuery()) {
                    if (!result.next()) throw new IllegalStateException("Exact administrator identity not found; no account changed.");
                }
            }
            try (var update = connection.prepareStatement("UPDATE dixit.appusers SET password=? WHERE id=? AND username=?")) {
                update.setString(1, new BCryptPasswordEncoder().encode(CharBuffer.wrap(password)));
                update.setInt(2, id); update.setString(3, username);
                if (update.executeUpdate() != 1) throw new IllegalStateException("Unexpected account update count.");
            }
            connection.commit();
        } catch (SQLException | RuntimeException error) { connection.rollback(); throw error; }
        finally { connection.setAutoCommit(true); }
    }
}
