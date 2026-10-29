package es.us.dp1.lx_xy_24_25.dixit.message;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.junit.jupiter.api.Test;

class MessageDTOTest {
    @Test
    void exposesStableMessageAndAccountIdentityWithoutParticipationOrPrivateGraphs() throws Exception {
        User user = new User(); user.setId(72); user.setUsername("Owl"); user.setPassword("private hash");
        Player player = new Player(); player.setId(901); player.setUser(user);
        Message message = new Message(); message.setId(17); message.setText("Hello"); message.setPlayer(player);
        var json = new ObjectMapper().valueToTree(new MessageDTO(message));
        assertThat(json.size()).isEqualTo(4);
        assertThat(json.get("id").asInt()).isEqualTo(17);
        assertThat(json.get("accountId").asInt()).isEqualTo(72);
        assertThat(json.get("username").asText()).isEqualTo("Owl");
        assertThat(json.get("text").asText()).isEqualTo("Hello");
        assertThat(json.has("player")).isFalse(); assertThat(json.has("password")).isFalse();
    }
}
