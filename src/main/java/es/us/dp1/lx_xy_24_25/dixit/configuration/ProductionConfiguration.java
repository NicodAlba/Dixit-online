package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.net.URI;
import java.util.Base64;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Validate before any datasource or JWT bean is instantiated; never echo values. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionConfiguration {
    @Bean
    static BeanFactoryPostProcessor validateProduction(Environment env) {
        return factory -> validate(env);
    }

    static void validate(Environment env) {
        String url = required(env, "spring.datasource.url");
        if (!url.startsWith("jdbc:postgresql://") || url.contains("@")
                || url.toLowerCase().matches(".*[?&](user|password)=.*")) {
            throw new IllegalStateException("Production requires a PostgreSQL JDBC URL without embedded credentials.");
        }
        try {
            URI database = URI.create(url.substring("jdbc:".length()));
            boolean local = java.util.Set.of("localhost", "127.0.0.1", "[::1]").contains(database.getHost() == null ? "" : database.getHost());
            if (database.getHost() == null || database.getPath() == null || database.getPath().length() < 2) throw new IllegalArgumentException();
            String ssl = env.getProperty("spring.datasource.hikari.data-source-properties.sslmode", "verify-full");
            if (!local && (!"verify-full".equals(ssl) || (database.getQuery() != null
                    && database.getQuery().toLowerCase().matches(".*(?:^|&)sslmode=(?!verify-full(?:&|$)).*")))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("Production database requires a valid host/database and verified TLS outside loopback.");
        }
        String runtimeUser = required(env, "spring.datasource.username");
        required(env, "spring.datasource.password");
        String migrationUser = required(env, "spring.flyway.user");
        required(env, "spring.flyway.password");
        if (runtimeUser.equals(migrationUser)) {
            throw new IllegalStateException("Production requires separate runtime and migration database roles.");
        }
        String runtimeRole = required(env, "dixit.database.runtime-role");
        if (!runtimeRole.matches("[a-z_][a-z0-9_]{0,62}")) {
            throw new IllegalStateException("Production runtime database role must be a simple PostgreSQL identifier.");
        }
        String key = required(env, "dp1.game.app.jwtSecret");
        try {
            if (Base64.getDecoder().decode(key).length < 64) throw new IllegalArgumentException();
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("Production JWT signing key must be private Base64 with at least 64 bytes.");
        }
        String origins = required(env, "dixit.web.allowed-origins");
        for (String value : origins.split(",", -1)) {
            try {
                URI origin = URI.create(value.trim());
                boolean local = "localhost".equals(origin.getHost()) || "127.0.0.1".equals(origin.getHost());
                if (origin.getHost() == null || origin.getUserInfo() != null || origin.getQuery() != null
                        || origin.getFragment() != null || !(origin.getPath() == null || origin.getPath().isEmpty())
                        || !("https".equals(origin.getScheme()) || (local && "http".equals(origin.getScheme())))) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException("Production allowed origins must be explicit HTTPS origins (HTTP loopback for isolated tests).");
            }
        }
        if (!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto"))
                || !"never".equals(env.getProperty("spring.sql.init.mode"))
                || env.getProperty("spring.h2.console.enabled", Boolean.class, false)
                || env.getProperty("spring.jpa.show-sql", Boolean.class, false)) {
            throw new IllegalStateException("Production requires schema validation, no SQL seeds, no H2 console and no SQL logging.");
        }
    }

    private static String required(Environment env, String property) {
        String value = env.getProperty(property);
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new IllegalStateException("Missing required production configuration: " + property);
        }
        return value;
    }
}
