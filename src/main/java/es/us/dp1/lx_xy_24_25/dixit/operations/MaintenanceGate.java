package es.us.dp1.lx_xy_24_25.dixit.operations;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** One-process admission gate. Already admitted transactions finish before closure returns. */
@Component
public class MaintenanceGate {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private boolean enabled;

    public MaintenanceGate(@Value("${dixit.maintenance.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        lock.readLock().lock();
        try { return enabled; }
        finally { lock.readLock().unlock(); }
    }

    public void setEnabled(boolean value) {
        lock.writeLock().lock();
        try { enabled = value; }
        finally { lock.writeLock().unlock(); }
    }

    public void admitNewGameOrStart() {
        lock.readLock().lock();
        boolean transactionOwnsLock = false;
        try {
            if (enabled) throw new MaintenanceUnavailableException();
            if (!TransactionSynchronizationManager.isActualTransactionActive()
                    || !TransactionSynchronizationManager.isSynchronizationActive()) {
                throw new IllegalStateException("Game admission requires an active transaction.");
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { lock.readLock().unlock(); }
            });
            transactionOwnsLock = true;
        } finally {
            if (!transactionOwnsLock) lock.readLock().unlock();
        }
    }
}
