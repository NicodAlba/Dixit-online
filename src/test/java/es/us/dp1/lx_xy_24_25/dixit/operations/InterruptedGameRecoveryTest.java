package es.us.dp1.lx_xy_24_25.dixit.operations;

import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterruptedGameRecoveryTest {
    @Test void ordinaryStartupClosesAdmissionsUntilRecoveryThenReopensButExplicitMaintenanceStaysClosed() {
        for (boolean planned : new boolean[]{false,true}) {
            GameService games=mock(GameService.class); MaintenanceGate gate=new MaintenanceGate(false);
            var runner=new InterruptedGameRecovery(games,gate,planned);
            assertTrue(gate.isEnabled());
            assertEquals("OUT_OF_SERVICE",runner.health().getStatus().getCode());
            when(games.cancelInterruptedGames()).thenAnswer(invocation->{assertTrue(gate.isEnabled());return 2;});
            runner.run(null);
            assertEquals("UP",runner.health().getStatus().getCode());
            verify(games).cancelInterruptedGames();assertEquals(planned,gate.isEnabled());
        }
    }
    @Test void failedRecoveryFailsStartupAndNeverOpensAdmissions() {
        GameService games=mock(GameService.class); MaintenanceGate gate=new MaintenanceGate(false);
        var runner=new InterruptedGameRecovery(games,gate,false);
        doThrow(new IllegalStateException("Synthetic failure")).when(games).cancelInterruptedGames();
        assertThrows(IllegalStateException.class,()->runner.run(null));assertTrue(gate.isEnabled());
        assertEquals("OUT_OF_SERVICE",runner.health().getStatus().getCode());
    }
}
