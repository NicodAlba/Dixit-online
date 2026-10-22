package es.us.dp1.lx_xy_24_25.dixit.card;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        List<Image> images = imageRepository.findAll();
        List<Card> deck = new ArrayList<>();
        for (Image image : images) {
            Card card = new Card();
            card.setGame(game);
            card.setImage(image);
            cardRepository.save(card);
            deck.add(card);
        }
        Collections.shuffle(deck);
        return deck;
    }
}
