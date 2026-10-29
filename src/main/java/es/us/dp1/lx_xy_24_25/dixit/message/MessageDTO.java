package es.us.dp1.lx_xy_24_25.dixit.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDTO {
    private Integer id;
    private String text;
    private String username;
    private Integer accountId;
    
    public MessageDTO(Message message) {
        this.id = message.getId();
        this.text = message.getText();
        this.accountId = message.getPlayer().getUser().getId();
        this.username = message.getPlayer().getUser().getUsername();
    }
}
