package es.us.dp1.lx_xy_24_25.dixit.auth.payload.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageResponse {

	 private String message;

	  public MessageResponse(String message) {
	    this.message = message;
	  }

}
