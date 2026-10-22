package es.us.dp1.lx_xy_24_25.dixit.achievement;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.Turn;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AchievementService {

    private AchievementRepository achievementRepository;
    private BadgeImageRepository badgeImageRepository;

    @Autowired
    public AchievementService(AchievementRepository achievementRepository, BadgeImageRepository badgeImageRepository){
        this.achievementRepository = achievementRepository;
        this.badgeImageRepository = badgeImageRepository;
    }



    @Transactional
    public List<Achievement> findAllAchievementsByUserId(Integer id){
        return achievementRepository.findAllAchievementsByUserId(id);
    }

    @Transactional
    public Achievement saveAchievement(Achievement achievement) throws DataAccessException{
        return achievementRepository.save(achievement);
    }

    @Transactional
    public List<Achievement> initializeWinAchievements(User user) {
        List<BadgeImage> images = badgeImageRepository.findByType(AchievementType.WIN);
        List<Achievement> achievements = new ArrayList<>();
        List<Integer> thresholds = new ArrayList<>();
        thresholds.add(1); thresholds.add(5); thresholds.add(10); thresholds.add(50);
        List<String> names = new ArrayList<>();
        names.add("The sweet taste of victory"); names.add("You're actually good in this"); names.add("Master storyteller"); names.add("God-like");
        for (int i = 0; i<images.size(); i++) {
            Achievement achievement = new WinAchievement();
            achievement.setUser(user);
            achievement.setBadgeImage(images.get(i));
            achievement.setThreshold(thresholds.get(i));
            achievement.setName(names.get(i));
            if (i == 0) {
                achievement.setDescription("Win " + achievement.getThreshold() + " game");
            } else {
                 achievement.setDescription("Win " + achievement.getThreshold() + " games");
            }
            achievementRepository.save(achievement);
            achievements.add(achievement);
        }
        return achievements;
    }
    @Transactional
    public List<Achievement> initializePlayAchievements(User user) {
        List<BadgeImage> images = badgeImageRepository.findByType(AchievementType.PLAY);
        List<Achievement> achievements = new ArrayList<>();
        List<Integer> thresholds = new ArrayList<>();
        thresholds.add(1);
        thresholds.add(5);
        thresholds.add(10);
        thresholds.add(50);
        List<String> names = new ArrayList<>();
        names.add("Funny, isn't it?"); names.add("Getting expertise"); names.add("Improving skills"); names.add("Tainted");
        for (int i = 0; i<images.size(); i++) {
            Achievement achievement = new PlayAchievement();
            achievement.setUser(user);
            achievement.setBadgeImage(images.get(i));
            achievement.setThreshold(thresholds.get(i));
            achievement.setName(names.get(i));
            if (i == 0) {
                achievement.setDescription("Play " + achievement.getThreshold() + " game");
            } else {
                 achievement.setDescription("Play " + achievement.getThreshold() + " games");
            }
            achievementRepository.save(achievement);
            achievements.add(achievement);
        }
        return achievements;
    }

    @Transactional
    public List<Achievement> initializeTrickAchievements(User user) {
        List<BadgeImage> images = badgeImageRepository.findByType(AchievementType.TRICK);
        List<Achievement> achievements = new ArrayList<>();
        List<Integer> thresholds = new ArrayList<>();
        thresholds.add(5); thresholds.add(20); thresholds.add(50); thresholds.add(100);
        List<String> names = new ArrayList<>();
        names.add("Initiating tricking skills"); names.add("Playing the imposter"); names.add("Loki would be proud"); names.add("The Trickster god");
        for (int i = 0; i<images.size(); i++) {
            Achievement achievement = new TrickAchievement();
            achievement.setUser(user);
            achievement.setBadgeImage(images.get(i));
            achievement.setThreshold(thresholds.get(i));
            achievement.setName(names.get(i));
            achievement.setDescription("Without being the Storyteller, achieve that at least " + achievement.getThreshold() + " players vote your card");
            achievementRepository.save(achievement);
            achievements.add(achievement);
        }
        return achievements;
    }

    @Transactional
    public void checkWinAchievements(User user) {
        List<Achievement> winAchievements = achievementRepository.findByUserAndType(user, WinAchievement.class);
        List<Game> games = user.getPlayers().stream().map(Player::getGame).filter(Objects::nonNull).toList();
        Integer winned = games.stream().filter(g -> g.getWinners().stream().filter(w -> w.getUser().getId().equals(user.getId())).count() == 1).toList().size();
       for (Achievement a : winAchievements) {
            ((WinAchievement) a).unlockWinAchievements(winned);
       }
    }

    @Transactional
    public void checkPlayAchievements(User user) {
        Integer played = (int) user.getPlayers().stream()
            .map(Player::getGame)
            .filter(Objects::nonNull)
            .filter(game -> game.getStartDate() != null && game.hasFinished())
            .map(Game::getId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        List<Achievement> playAchievements = achievementRepository.findByUserAndType(user, PlayAchievement.class);
        for (Achievement a : playAchievements) {
            ((PlayAchievement) a).unlockPlayAchievements(played);
        }
    }

    @Transactional
    public void checkTrickAchievements(User user) {
        List<Game> games = user.getPlayers().stream().map(Player::getGame).filter(Objects::nonNull).toList();
        List<Turn> turns = games.stream().flatMap(g -> g.getTurns().stream()).toList();
        List<Turn> noStorytellerTurns = turns.stream().filter(t -> !(t.getStoryteller().getUser().getId().equals(user.getId()))).toList();
        Integer votes = noStorytellerTurns.stream().flatMap(t -> t.getPlayedCards().stream()).filter(c -> c.getUser().getId().equals(user.getId()))
                        .mapToInt(c -> c.getVotes().size()).sum();
        List<Achievement> trickAchievements = achievementRepository.findByUserAndType(user, TrickAchievement.class);
        for (Achievement a : trickAchievements){
            ((TrickAchievement) a).unlockTrickAchievements(votes);
        }
    }
}
