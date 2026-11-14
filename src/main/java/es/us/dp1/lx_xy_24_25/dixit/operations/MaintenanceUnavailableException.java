package es.us.dp1.lx_xy_24_25.dixit.operations;

public class MaintenanceUnavailableException extends RuntimeException {
    public MaintenanceUnavailableException() {
        super("New games are temporarily unavailable for maintenance.");
    }
}
