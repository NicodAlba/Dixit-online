package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameViewHandOrderTest {
    @Test
    void handViewSurvivesDifferentDatabaseOrderWithoutRevealingHistoricalOrRivalCards() {
        User user = new User(); user.setId(1); user.setUsername("owner");
        Player owner = new Player(); owner.setId(10); owner.setUser(user);
        Player rival = new Player(); rival.setId(20);
        Card first = card(100, owner), second = card(200, owner);
        Card historical = card(300, owner); historical.setTurn(new Turn());
        Card foreign = card(400, rival);
        owner.setCards(List.of(second, historical, foreign, first));
        var before = GameView.participant(owner, owner.getId());
        owner.setCards(List.of(first, foreign, second, historical));
        assertEquals(before, GameView.participant(owner, owner.getId()));
        assertEquals(List.of(100, 200), before.cards().stream().map(GameView.PlayedCard::id).toList());
        assertEquals(2, before.handSize());
        assertTrue(GameView.participant(owner, rival.getId()).cards().isEmpty());
    }

    private Card card(int id, Player owner) {
        Card card = new Card(); card.setId(id); card.setOwner(owner); return card;
    }
}
