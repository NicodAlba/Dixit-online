package es.us.dp1.lx_xy_24_25.dixit.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "There are already 4 players in this game")
public class TooManyPlayersException extends Exception {
}
