package es.us.dp1.lx_xy_24_25.dixit.achievement;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayAchievementsCountTest {
    private final AchievementRepository repository = mock(AchievementRepository.class);
    private final AchievementService service = new AchievementService(repository, mock(BadgeImageRepository.class));
    private final User user = new User();
    private final List<Achievement> achievements = new ArrayList<>();
    private PlayAchievement firstGame;

    @BeforeEach
    void setup() {
        user.setId(1);
        firstGame = achievement(1);
        when(repository.findByUserAndType(user, PlayAchievement.class)).thenReturn(achievements);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_GAME", "LOBBY", "IN_PROGRESS"})
    void doesNotCountUnfinishedParticipations(String state) {
        Game game = switch (state) {
            case "NO_GAME" -> null;
            case "LOBBY" -> game(10, false, false);
            case "IN_PROGRESS" -> game(10, true, false);
            default -> throw new IllegalArgumentException(state);
        };
        participate(game);

        service.checkPlayAchievements(user);

        assertNull(firstGame.getDateObtained(), state + " must not unlock the first played game");
    }

    @Test
    void countsTheSameGameIdOnceEvenWithDifferentEntityInstances() {
        PlayAchievement secondGame = achievement(2);
        participate(game(10, true, true));
        participate(game(10, true, true));

        service.checkPlayAchievements(user);

        assertNotNull(firstGame.getDateObtained());
        assertNull(secondGame.getDateObtained(), "Duplicate participations must not count as two games");
    }

    @Test
    void countsDistinctCompletedGames() {
        PlayAchievement secondGame = achievement(2);
        PlayAchievement thirdGame = achievement(3);
        participate(game(10, true, true));
        participate(game(11, true, true));

        service.checkPlayAchievements(user);

        assertNotNull(firstGame.getDateObtained());
        assertNotNull(secondGame.getDateObtained());
        assertNull(thirdGame.getDateObtained());
    }

    @Test
    void countsOnlyCompletedGamesInMixedHistory() {
        PlayAchievement secondGame = achievement(2);
        PlayAchievement thirdGame = achievement(3);
        participate(null);
        participate(game(10, false, false));
        participate(game(11, true, false));
        participate(game(12, true, true));
        participate(game(12, true, true));
        participate(game(13, true, true));

        service.checkPlayAchievements(user);

        assertNotNull(firstGame.getDateObtained());
        assertNotNull(secondGame.getDateObtained());
        assertNull(thirdGame.getDateObtained(), "Six participation records contain only two completed games");
    }

    @Test
    void doesNotCountAnUnsavedGame() {
        participate(game(null, true, true));

        service.checkPlayAchievements(user);

        assertNull(firstGame.getDateObtained());
    }

    @Test
    void preservesAnObtainedDateEvenWhenNoCompletedGamesRemain() {
        Date obtained = new Date(1_000);
        firstGame.setDateObtained(obtained);

        service.checkPlayAchievements(user);

        assertSame(obtained, firstGame.getDateObtained());
    }

    @Test
    void repeatedChecksDoNotReplaceTheUnlockDate() {
        participate(game(10, true, true));
        service.checkPlayAchievements(user);
        Date obtained = firstGame.getDateObtained();
        assertNotNull(obtained);

        service.checkPlayAchievements(user);

        assertSame(obtained, firstGame.getDateObtained());
    }

    private PlayAchievement achievement(int threshold) {
        PlayAchievement achievement = new PlayAchievement();
        achievement.setUser(user);
        achievement.setThreshold(threshold);
        achievements.add(achievement);
        return achievement;
    }

    private void participate(Game game) {
        Player player = new Player();
        player.setUser(user);
        player.setGame(game);
        user.getPlayers().add(player);
    }

    private Game game(Integer id, boolean started, boolean finished) {
        Game game = new Game();
        game.setId(id);
        if (started) {
            game.setPlayers(List.of(new Player(), new Player(), new Player()));
            game.setStartDate(new Date(1_000));
        }
        if (finished) {
            game.setEndDate(new Date(2_000));
        }
        return game;
    }
}
