package es.us.dp1.lx_xy_24_25.dixit.card;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.TurnState;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CardService {
    private final ImageRepository imageRepository;
    private final CardRepository cardRepository;

    @Autowired
    public CardService(ImageRepository imageRepository, CardRepository cardRepository) {
        this.imageRepository = imageRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public List<Card> initializeDeck(Game game) {
        // Initialize the inverse collection before inserts, keeping snapshots consistent with reloads.
        if (!game.getDeck().isEmpty()) throw new IllegalStateException("The game deck is already initialized");
        List<Image> images = imageRepository.findAll();
        List<Card> deck = new ArrayList<>();
        for (Image image : images) {
            Card card = new Card();
            card.setGame(game);
            card.setImage(image);
            cardRepository.save(card);
            game.getDeck().add(card);
            deck.add(card);
        }
        Collections.shuffle(deck);
        return deck;
    }

    private record Candidate(Image image, Card unused) {}

    // Count and draw use the same image pool; historical aliases never add availability.
    private List<Candidate> availableImages(Game game, List<Card> cards) {
        Set<Integer> blocked = new HashSet<>();
        Map<Integer, Candidate> candidates = new HashMap<>();
        for (Card card : cards) {
            if (card.getImage() == null) throw new IllegalStateException("A game card has no image");
            if (card.getOwner() != null && (card.getOwner().getGame() == null
                || !Objects.equals(card.getOwner().getGame().getId(), game.getId()) || card.getUser() != null
                || (card.getTurn() != null && card.getTurn().getState() == TurnState.FINISHED))) {
                throw new IllegalStateException("Invalid ownership of a game card");
            }
            if (card.getTurn() != null && (card.getTurn().getGame() == null
                || !Objects.equals(card.getTurn().getGame().getId(), game.getId()))) {
                throw new IllegalStateException("Card belongs to a different game's turn");
            }
            Integer imageId = card.getImage().getId();
            boolean activeTable = card.getTurn() != null && card.getTurn().getState() != TurnState.FINISHED;
            if (card.getOwner() != null || activeTable) {
                if (!blocked.add(imageId)) throw new IllegalStateException("Duplicate image in active hands or table");
            } else if (card.getTurn() == null) {
                if (card.getUser() != null || !card.getVotes().isEmpty()) throw new IllegalStateException("Unused card contains historical data");
                Candidate previous = candidates.put(imageId, new Candidate(card.getImage(), card));
                if (previous != null && previous.unused() != null) throw new IllegalStateException("Duplicate unused image");
            } else {
                if (card.getUser() == null) throw new IllegalStateException("Finished card has no author");
                candidates.putIfAbsent(imageId, new Candidate(card.getImage(), null));
            }
        }
        blocked.forEach(candidates::remove);
        return new ArrayList<>(candidates.values());
    }

    @Transactional(readOnly = true)
    public int countAvailableImages(Game game) {
        return availableImages(game, cardRepository.findByGameId(game.getId())).size();
    }

    @Transactional
    public void replenishHands(Game game) {
        List<Card> cards = cardRepository.findByGameId(game.getId());
        for (Player player : game.getPlayers()) {
            long handSize = cards.stream().filter(c -> c.getOwner() != null
                && Objects.equals(c.getOwner().getId(), player.getId()) && c.getTurn() == null).count();
            if (handSize != 5) throw new IllegalStateException("Every player must have five cards before replenishment");
        }
        List<Candidate> candidates = availableImages(game, cards);
        List<Candidate> unused = new ArrayList<>(candidates.stream().filter(c -> c.unused() != null).toList());
        List<Candidate> recycled = new ArrayList<>(candidates.stream().filter(c -> c.unused() == null).toList());
        Collections.shuffle(unused);
        Collections.shuffle(recycled);
        unused.addAll(recycled);
        if (unused.size() < game.getPlayers().size()) throw new IllegalStateException("Not enough distinct images to replenish every hand");
        // Validate the complete plan before assigning owners or creating occurrences.
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Candidate candidate = unused.get(i);
            Card card = candidate.unused();
            if (card == null) {
                card = new Card();
                card.setGame(game);
                card.setImage(candidate.image());
                game.getDeck().add(card);
            }
            game.getPlayers().get(i).receiveCard(card);
            cardRepository.save(card);
        }
    }
}
