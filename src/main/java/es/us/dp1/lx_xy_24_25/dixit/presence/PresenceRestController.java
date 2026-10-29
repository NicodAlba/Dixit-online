package es.us.dp1.lx_xy_24_25.dixit.presence;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/presence")
public class PresenceRestController {
    private final PresenceService presence;
    public PresenceRestController(PresenceService presence){this.presence=presence;}
    @GetMapping
    public List<PresenceService.Connection> visibleConnections(@RequestParam(required=false) Integer gameId) {
        return presence.visibleConnections(gameId);
    }
}
