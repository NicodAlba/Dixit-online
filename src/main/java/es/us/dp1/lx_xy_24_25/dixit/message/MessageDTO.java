package es.us.dp1.lx_xy_24_25.dixit.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDTO {
    private String text;
    private String username;
    
    public MessageDTO(Message message) {
        this.text = message.getText();
        this.username = message.getPlayer().getUser().getUsername();
    }
}
