package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.SQLException;
import java.util.UUID;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Fence every runtime physical session, including Hikari replacement connections. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class SingleInstanceConnections {
    @Bean(name = "runtimeInstanceConnectionGuard")
    static BeanPostProcessor singleInstanceConnections(Environment environment) {
        boolean enabled = environment.getProperty("dixit.database.single-instance-guard", Boolean.class, false);
        String process = "dixit.runtime." + UUID.randomUUID();
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (enabled && "dataSource".equals(name)) {
                    if (!(bean instanceof HikariDataSource pool)) {
                        throw new IllegalStateException("Single-instance protection requires the runtime Hikari datasource.");
                    }
                    configure(pool, process);
                    // Claim before the datasource can be injected into Flyway/JPA or startup recovery.
                    try (var connection = pool.getConnection()) {
                        if (!connection.isValid(10)) throw new SQLException("Invalid guarded database connection");
                    } catch (SQLException failure) {
                        pool.close();
                        throw new IllegalStateException("Cannot acquire the Dixit database instance guard.", failure);
                    }
                }
                return bean;
            }
        };
    }

    static void configure(HikariDataSource pool, String process) {
        if (pool.getConnectionInitSql() != null && !pool.getConnectionInitSql().isBlank()) {
            throw new IllegalStateException("The instance guard requires exclusive ownership of connection initialization.");
        }
        pool.setConnectionInitSql(initializationSql(process));
        pool.setMinimumIdle(Math.max(1, pool.getMinimumIdle()));
    }

    static String initializationSql(String process) {
        if (!process.matches("dixit[.]runtime[.][0-9a-f-]{36}")) {
            throw new IllegalArgumentException("Invalid internal database process identity.");
        }
        // Distinct two-int lock keys, scoped by PostgreSQL to this database. A transaction
        // mutex serializes registration; every accepted physical session retains a shared
        // cohort lock until it closes. Losing one session cannot free the other sessions.
        return """
            DO $dixit_guard$
            BEGIN
                PERFORM pg_catalog.pg_advisory_xact_lock(1145657417, 1280262987);
                PERFORM pg_catalog.pg_stat_clear_snapshot();
                IF EXISTS (
                    SELECT 1 FROM pg_catalog.pg_locks l
                    LEFT JOIN pg_catalog.pg_stat_activity a ON a.pid = l.pid
                    WHERE l.locktype = 'advisory' AND l.granted
                      AND l.database = (SELECT oid FROM pg_catalog.pg_database WHERE datname = current_database())
                      AND l.classid = 1145657417::oid AND l.objid = 1414681934::oid AND l.objsubid = 2
                      AND a.application_name IS DISTINCT FROM '%s'
                ) THEN
                    RAISE EXCEPTION 'Another Dixit process owns the database instance guard' USING ERRCODE = '55000';
                END IF;
                PERFORM pg_catalog.set_config('application_name', '%s', false);
                PERFORM pg_catalog.pg_advisory_lock_shared(1145657417, 1414681934);
            END
            $dixit_guard$;
            """.formatted(process, process);
    }
}
