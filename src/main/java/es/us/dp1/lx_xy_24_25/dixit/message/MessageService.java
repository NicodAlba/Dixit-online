package es.us.dp1.lx_xy_24_25.dixit.message;

import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.game.GameRepository;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    MessageRepository messageRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private GameRepository gameRepository;

    public MessageService(MessageRepository messageRepository, PlayerRepository playerRepository, GameRepository gameRepository) {
        this.messageRepository = messageRepository;
        this.playerRepository = playerRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public List<Message> findMessagesByPlayerId(Integer id) {
        return messageRepository.findByPlayer(playerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Player", "id", id)));
    }

    @Transactional(readOnly = true)
    public List<Message> findMessagesByGameId(Integer id) {
        return messageRepository.findByGame(gameRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Game", "id", id)));
    }

    @Transactional
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

}
