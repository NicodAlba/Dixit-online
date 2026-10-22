package es.us.dp1.lx_xy_24_25.dixit.card;

import es.us.dp1.lx_xy_24_25.dixit.game.*;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardReplenishmentTest {
    private final CardRepository cards = mock(CardRepository.class);
    private final CardService service = new CardService(mock(ImageRepository.class), cards);

    private Game fixture() {
        Game game = new Game(); game.setId(1);
        for (int i = 0; i < 4; i++) {
            Player player = new Player(); player.setId(100 + i); player.setGame(game);
            User user = new User(); user.setId(200 + i); player.setUser(user);
            game.getPlayers().add(player);
        }
        Turn history = new Turn(); history.setId(300); history.setGame(game); history.setState(TurnState.FINISHED);
        for (int i = 1; i <= 36; i++) {
            Image image = new Image(); image.setId(i);
            Card card = new Card(); card.setId(i); card.setImage(image); card.setGame(game);
            if (i <= 20) game.getPlayers().get((i - 1) / 5).receiveCard(card);
            else if (i > 22) {
                card.setTurn(history); card.setUser(game.getPlayers().getFirst().getUser());
                card.vote(game.getPlayers().getLast()); history.getPlayedCards().add(card);
            }
            game.getDeck().add(card);
        }
        when(cards.findByGameId(1)).thenAnswer(inv -> new ArrayList<>(game.getDeck()));
        when(cards.save(any(Card.class))).thenAnswer(inv -> {
            Card card = inv.getArgument(0);
            if (card.getId() == null) card.setId(1000 + game.getDeck().indexOf(card));
            return card;
        });
        return game;
    }

    @Test
    void usesRemainingUnusedCardsAndCreatesOnlyMissingOccurrences() {
        Game game = fixture();
        List<Card> history = game.getDeck().stream().filter(c -> c.getTurn() != null).toList();
        assertEquals(16, service.countAvailableImages(game));
        service.replenishHands(game);
        assertEquals(38, game.getDeck().size());
        Set<Integer> activeImages = new HashSet<>(), activeIds = new HashSet<>();
        for (Player player : game.getPlayers()) {
            assertEquals(6, player.getCards().size());
            for (Card card : player.getCards()) {
                assertTrue(activeImages.add(card.getImage().getId())); activeIds.add(card.getId());
                assertNull(card.getTurn()); assertNull(card.getUser()); assertTrue(card.getVotes().isEmpty());
            }
        }
        assertTrue(activeIds.containsAll(List.of(21, 22)), "Use both remaining new cards before recycling");
        assertEquals(2, activeIds.stream().filter(id -> id >= 1000).count());
        assertEquals(12, service.countAvailableImages(game), "Historical aliases do not add image availability");
        for (Card historical : history) {
            assertEquals(300, historical.getTurn().getId()); assertNull(historical.getOwner());
            assertEquals(200, historical.getUser().getId()); assertEquals(1, historical.getVotes().size());
        }
    }

    @Test
    void activeTableImageIsUnavailableEvenWithoutOwner() {
        Game game = fixture();
        Card onTable = game.getDeck().get(20);
        Turn active = new Turn(); active.setId(301); active.setGame(game); active.setState(TurnState.SHOWING_CARDS);
        onTable.setTurn(active);
        assertEquals(15, service.countAvailableImages(game));
        service.replenishHands(game);
        assertTrue(game.getPlayers().stream().flatMap(p -> p.getCards().stream()).noneMatch(c -> c.getImage().getId().equals(21)));
    }

    @Test
    void insufficientPoolCannotPartiallyAssignCards() {
        Game game = fixture();
        when(cards.findByGameId(1)).thenReturn(game.getDeck().stream().filter(c -> c.getImage().getId() <= 23).toList());
        assertEquals(3, service.countAvailableImages(game));
        assertThrows(IllegalStateException.class, () -> service.replenishHands(game));
        assertTrue(game.getPlayers().stream().allMatch(p -> p.getCards().size() == 5));
        verify(cards, never()).save(any(Card.class));
    }
}
