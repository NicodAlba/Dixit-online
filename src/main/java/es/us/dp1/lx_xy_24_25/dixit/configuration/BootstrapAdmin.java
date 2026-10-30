package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.io.Console;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit local CLI only. No Spring bean, default account, password argument or public endpoint. */
public final class BootstrapAdmin {
    private BootstrapAdmin() {}

    public static void main(String[] args) {
        try {
            Console console = System.console();
            if (args.length != 0 || console == null) {
                throw new IllegalArgumentException("Use an interactive terminal; passwords are accepted only through hidden console input.");
            }
            Map<String,String> env = System.getenv();
            String url = required(env, "DIXIT_DB_URL");
            String ssl = env.getOrDefault("DIXIT_DB_SSL_MODE", "verify-full");
            ProductionConfiguration.validateDatabaseUrl(url, ssl);
            Properties properties = new Properties();
            properties.setProperty("user", required(env, "DIXIT_DB_USERNAME"));
            properties.setProperty("password", required(env, "DIXIT_DB_PASSWORD"));
            properties.setProperty("sslmode", ssl);
            properties.setProperty("sslrootcert", env.getOrDefault("DIXIT_DB_SSL_ROOT_CERT", System.getProperty("user.home") + "/.postgresql/root.crt"));
            properties.setProperty("connectTimeout", "10");
            String username = console.readLine("Administrator username: ");
            char[] password = console.readPassword("Administrator password (12+ characters): ");
            char[] confirmation = console.readPassword("Confirm password: ");
            try (Connection connection = DriverManager.getConnection(url, properties)) {
                create(connection, username, password, confirmation);
                console.printf("Administrator created. No existing account was modified.%n");
            } finally {
                if (password != null) Arrays.fill(password, '\0');
                if (confirmation != null) Arrays.fill(confirmation, '\0');
            }
        } catch (SQLException error) {
            System.err.println("Administrator bootstrap failed (SQL state " + error.getSQLState() + "); no credentials or SQL printed.");
            System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    static int create(Connection connection, String username, char[] password, char[] confirmation) throws SQLException {
        validateInput(username, password, confirmation);
        if (!connection.getAutoCommit()) throw new IllegalStateException("Bootstrap requires a dedicated connection, not an existing transaction.");
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                // A transaction-scoped advisory lock serializes competing bootstrap commands.
                // Runtime can read authorities but cannot UPDATE/lock a catalog row FOR UPDATE.
                statement.execute("SELECT pg_advisory_xact_lock(hashtextextended('dixit.first-administrator',0))");
                try (ResultSet role = statement.executeQuery("SELECT rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls "
                        + "OR has_schema_privilege(current_user,'dixit','CREATE') OR has_schema_privilege(current_user,'public','CREATE') "
                        + "OR has_database_privilege(current_user,current_database(),'CREATE') FROM pg_roles WHERE rolname=current_user")) {
                    if (!role.next() || role.getBoolean(1)) throw new IllegalStateException("Use the restricted application database role for bootstrap.");
                }
                try (ResultSet admins = statement.executeQuery("SELECT count(*) FROM dixit.appusers u JOIN dixit.authorities a ON a.id=u.authority_id WHERE a.authority='ADMIN'")) {
                    admins.next();
                    if (admins.getInt(1) != 0) throw new IllegalStateException("An administrator already exists; no account was changed.");
                }
            }
            int authority;
            try (Statement statement = connection.createStatement();
                 ResultSet catalog = statement.executeQuery("SELECT id FROM dixit.authorities WHERE authority='ADMIN'")) {
                if (!catalog.next()) throw new IllegalStateException("Apply the approved reference migrations before bootstrap.");
                authority = catalog.getInt(1);
                if (catalog.next()) throw new IllegalStateException("Administrator authority is ambiguous; no account was created.");
            }
            String hash = new BCryptPasswordEncoder().encode(CharBuffer.wrap(password));
            int id;
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO dixit.appusers(id,username,password,authority_id) "
                    + "VALUES(nextval('dixit.entity_sequence'),?,?,?) RETURNING id")) {
                insert.setString(1, username); insert.setString(2, hash); insert.setInt(3, authority);
                try (ResultSet result = insert.executeQuery()) { result.next(); id = result.getInt(1); }
            }
            connection.commit();
            return id;
        } catch (SQLException | RuntimeException error) {
            connection.rollback();
            throw error;
        } finally { connection.setAutoCommit(true); }
    }

    static void validateInput(String username, char[] password, char[] confirmation) {
        if (username == null || username.isBlank() || username.length() > 50 || !username.equals(username.trim())) {
            throw new IllegalArgumentException("Choose a nonblank administrator username of at most 50 characters without surrounding spaces.");
        }
        if (password == null || confirmation == null || password.length < 12 || !Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Passwords must match and contain at least 12 characters.");
        }
        var encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        int bytes = encoded.remaining();
        for (int index = encoded.position(); index < encoded.limit(); index++) encoded.put(index, (byte)0);
        if (bytes > 72) throw new IllegalArgumentException("Password exceeds BCrypt's 72-byte limit.");
    }

    private static String required(Map<String,String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing required backend configuration: " + name);
        return value;
    }
}
