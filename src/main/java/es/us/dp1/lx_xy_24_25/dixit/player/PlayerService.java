package es.us.dp1.lx_xy_24_25.dixit.player;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public List<Player> findAll() {
        return (List<Player>)  playerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Player findById(Integer id) {
        return playerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Player", "id", id));
    }

    @Transactional
    public Player savePlayer(@Valid Player player) throws DataAccessException {
        return playerRepository.save(player);
    }

    @Transactional(readOnly = true)
    public List<Card> getCards(Player player) {
        return player.getCards();
    }

}
