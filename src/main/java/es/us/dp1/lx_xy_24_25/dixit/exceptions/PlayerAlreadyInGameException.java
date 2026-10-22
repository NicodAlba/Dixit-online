package es.us.dp1.lx_xy_24_25.dixit.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "You're already in this game")
public class PlayerAlreadyInGameException extends Exception {
}
