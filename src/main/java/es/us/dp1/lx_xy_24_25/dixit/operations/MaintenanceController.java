package es.us.dp1.lx_xy_24_25.dixit.operations;

import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.InvalidRequestException;
import es.us.dp1.lx_xy_24_25.dixit.game.GameRepository;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestController
@RequestMapping("/api/v1/operations/maintenance")
public class MaintenanceController {
    private final MaintenanceGate gate;
    private final GameRepository games;
    private final UserService users;
    public record Status(boolean enabled, long activeGameCount, long waitingLobbyCount) {}

    public MaintenanceController(MaintenanceGate gate, GameRepository games, UserService users) {
        this.gate = gate; this.games = games; this.users = users;
    }
    private void requireAdmin() {
        if (!users.findCurrentUser().hasAuthority("ADMIN")) throw new AccessDeniedException();
    }
    private Status status() {
        return new Status(gate.isEnabled(), games.countByStartDateIsNotNullAndEndDateIsNull(),
            games.countByStartDateIsNullAndEndDateIsNull());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalidBody() { return Map.of("message", "Maintenance requires exactly one boolean enabled field."); }
    @GetMapping public Status get() { requireAdmin(); return status(); }
    @PutMapping public Status update(@RequestBody Map<String,Object> body) {
        requireAdmin();
        if (body == null || !body.keySet().equals(Set.of("enabled")) || !(body.get("enabled") instanceof Boolean value)) {
            throw new InvalidRequestException("Maintenance requires exactly one boolean enabled field.");
        }
        gate.setEnabled(value);
        return status();
    }
}
