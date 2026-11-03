package es.us.dp1.lx_xy_24_25.dixit.operations;

import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MaintenanceTransactionTest {
    @Autowired MaintenanceGate gate;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @AfterEach void reopen() { gate.setEnabled(false); }

    @Test void closureWaitsForAdmittedTransactionAndThenDeniesNewAdmissions() throws Exception {
        CountDownLatch admitted = new CountDownLatch(1), commit = new CountDownLatch(1), closing = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> transaction = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.queryForObject("select 1", Integer.class);
                gate.admitNewGameOrStart();
                admitted.countDown();
                try { assertTrue(commit.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
            }));
            assertTrue(admitted.await(5, TimeUnit.SECONDS));
            Future<?> closure = workers.submit(() -> { closing.countDown(); gate.setEnabled(true); });
            assertTrue(closing.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> closure.get(200, TimeUnit.MILLISECONDS));
            commit.countDown();
            transaction.get(5, TimeUnit.SECONDS); closure.get(5, TimeUnit.SECONDS);
            assertTrue(gate.isEnabled());
            assertThrows(MaintenanceUnavailableException.class, () -> new TransactionTemplate(transactions)
                .executeWithoutResult(status -> gate.admitNewGameOrStart()));
        } finally { commit.countDown(); workers.shutdownNow(); }
    }

    @Test void rollbackReleasesAdmissionAndExplicitClosedStartupRejectsImmediately() {
        assertThrows(IllegalArgumentException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            gate.admitNewGameOrStart();
            throw new IllegalArgumentException("Synthetic rollback");
        }));
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> gate.setEnabled(true));
        assertThrows(MaintenanceUnavailableException.class, () -> new MaintenanceGate(true).admitNewGameOrStart());
    }
}
