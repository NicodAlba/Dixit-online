package es.us.dp1.lx_xy_24_25.dixit.exceptions;

/** Invalid client fields, distinct from an unexpected internal argument failure. */
public class InvalidRequestException extends IllegalArgumentException {
    public InvalidRequestException(String message) { super(message); }
}
