package es.us.dp1.lx_xy_24_25.dixit.operations;

import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** One instance only: never run this recovery alongside another playable process. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name="dixit.recover-interrupted-games", havingValue="true")
public class InterruptedGameRecovery implements ApplicationRunner, HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(InterruptedGameRecovery.class);
    private final GameService games;
    private final MaintenanceGate maintenance;
    private final boolean remainClosed;
    private volatile boolean recovered;

    public InterruptedGameRecovery(GameService games, MaintenanceGate maintenance,
            @Value("${dixit.maintenance.enabled:false}") boolean remainClosed) {
        this.games = games;
        this.maintenance = maintenance;
        this.remainClosed = remainClosed;
        maintenance.setEnabled(true);
    }

    @Override public Health health() { return recovered ? Health.up().build() : Health.outOfService().build(); }

    @Override public void run(ApplicationArguments arguments) {
        int cancelled = games.cancelInterruptedGames();
        // Failure propagates: startup fails and admissions stay closed.
        maintenance.setEnabled(remainClosed);
        recovered = true;
        log.info("Startup recovery completed: {} interrupted games cancelled; maintenance={}", cancelled, remainClosed);
    }
}
