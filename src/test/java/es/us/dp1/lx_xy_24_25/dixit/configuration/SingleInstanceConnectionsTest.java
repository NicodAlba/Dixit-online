package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SingleInstanceConnectionsTest {
    @Test void configurationAndPostprocessorHaveDistinctSpringBeanNames() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(SingleInstanceConnections.class);
            context.refresh();
            assertNotNull(context.getBean("singleInstanceConnections"));
            assertNotNull(context.getBean("runtimeInstanceConnectionGuard"));
        }
    }
    @Test void claimsBeforeReturningTheDatasourceToItsConsumers() throws Exception {
        var pool = mock(HikariDataSource.class);
        var connection = mock(Connection.class);
        when(pool.getConnection()).thenReturn(connection);
        when(connection.isValid(10)).thenReturn(true);
        var processor = SingleInstanceConnections.singleInstanceConnections(
            new MockEnvironment().withProperty("dixit.database.single-instance-guard", "true"));
        assertSame(pool, processor.postProcessAfterInitialization(pool, "dataSource"));
        var order = inOrder(pool, connection);
        order.verify(pool).getConnectionInitSql();
        order.verify(pool).setConnectionInitSql(contains("pg_advisory_lock_shared"));
        order.verify(pool).getMinimumIdle(); order.verify(pool).setMinimumIdle(1);
        order.verify(pool).getConnection(); order.verify(connection).isValid(10); order.verify(connection).close();
    }
    @Test void failedClaimClosesThePoolAndPreventsStartup() throws Exception {
        var pool = mock(HikariDataSource.class);
        when(pool.getConnection()).thenThrow(new SQLException("Controlled conflict", "55000"));
        var processor = SingleInstanceConnections.singleInstanceConnections(
            new MockEnvironment().withProperty("dixit.database.single-instance-guard", "true"));
        assertThrows(IllegalStateException.class, () -> processor.postProcessAfterInitialization(pool, "dataSource"));
        verify(pool).close();
    }
    @Test void disabledGuardDoesNotTouchDevelopmentOrIndependentTools() {
        var pool = mock(HikariDataSource.class);
        var processor = SingleInstanceConnections.singleInstanceConnections(new MockEnvironment());
        assertSame(pool, processor.postProcessAfterInitialization(pool, "dataSource"));
        verifyNoInteractions(pool);
    }
}
