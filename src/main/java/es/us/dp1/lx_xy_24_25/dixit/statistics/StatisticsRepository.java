package es.us.dp1.lx_xy_24_25.dixit.statistics;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import es.us.dp1.lx_xy_24_25.dixit.card.Image;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;

@Repository
public interface StatisticsRepository extends JpaRepository<Game, Integer> {

    //----------------------number of games queries-------------------------

    @Query("SELECT COUNT(g) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Long countCompletedGames();

    @Query("SELECT COUNT(p) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false AND p.user.id = :userId")
    Long countCompletedGamesByUserId(Integer userId);

    @Query("SELECT CASE WHEN COUNT(DISTINCT p.user.id) = 0 THEN 0.0 ELSE COUNT(p) * 1.0 / COUNT(DISTINCT p.user.id) END FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Double averageGamesPerUser();

    @Query("SELECT COUNT(p) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false GROUP BY p.user.id ORDER BY COUNT(p) DESC LIMIT 1")
    Long maximumGamesByUser();

    @Query("SELECT COUNT(p) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false GROUP BY p.user.id ORDER BY COUNT(p) ASC LIMIT 1")
    Long minimumGamesByUser(); //we take into consideration only users that have played at least one game

    //----------------------duration queries-------------------------

    @Query("SELECT SUM(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Long totalDuration();

    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Double averageDuration();

    @Query("SELECT MAX(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Long maximumDuration();

    @Query("SELECT MIN(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Long minimumDuration();

    @Query("SELECT SUM(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false AND p.user.id = :userId")
    Long totalDurationByUserId(Integer userId);

    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false AND p.user.id = :userId")
    Double averageDurationByUserId(Integer userId);

    @Query("SELECT MIN(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false AND p.user.id = :userId")
    Long minimumDurationByUserId(Integer userId);

    @Query("SELECT MAX(TIMESTAMPDIFF(MINUTE, g.startDate, g.endDate)) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false AND p.user.id = :userId")
    Long maximumDurationByUserId(Integer userId);

    //----------------------players queries-------------------------

    @Query("SELECT COUNT(DISTINCT p.user.id) FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Long totalUsersThatHavePlayed();

    @Query("SELECT AVG(SIZE(g.players)) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Double averagePlayersPerGame();

    @Query("SELECT MAX(SIZE(g.players)) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Long maximumPlayersInGame();

    @Query("SELECT MIN(SIZE(g.players)) FROM Game g WHERE g.endDate IS NOT NULL AND g.cancelled = false")
    Long minimumPlayersInGame();

    @Query("SELECT COUNT(DISTINCT p.user.id) FROM Game g JOIN g.players p WHERE g.endDate IS NULL")
    Long currentUsersPlaying();

    //----------------------specific user statistics in games-------------------------

    @Query("SELECT c.image FROM Card c WHERE c.user.id = :userId GROUP BY c.image ORDER BY COUNT(c) DESC LIMIT 1")
    Image findMostPlayedCardByUserId(Integer userId);

    @Query("SELECT COUNT(g) FROM Game g JOIN g.winners w WHERE w.user.id = :userId AND g.endDate IS NOT NULL AND g.cancelled = false")
    Long countWinsByUserId(Integer userId);

    //win rate computed in service

    @Query("SELECT AVG(p.points) FROM Game g JOIN g.players p WHERE p.user.id = :userId AND g.endDate IS NOT NULL AND g.cancelled = false")
    Double averagePointsPerGameByUserId(Integer userId);

    @Query("SELECT SUM(p.points) FROM Game g JOIN g.players p WHERE p.user.id = :userId AND g.endDate IS NOT NULL AND g.cancelled = false")
    Long totalPointsByUserId(Integer userId);

    @Query("SELECT COUNT(v) FROM Card c JOIN c.votes v JOIN c.turn t WHERE c.user.id = :userId AND t.storyteller.id != :userId")
    Long totalTrapVotesReceivedByUserId(Integer userId);

    //helper for deception rate
    @Query("SELECT COUNT(c) FROM Turn t JOIN t.playedCards c WHERE c.user.id = :userId AND t.storyteller.id != :userId")
    Long totalTurnsAsNormalPlayerByUserId(Integer userId);

    //deception rate computed in service

    //----------------------rankings-------------------------

    @Query("SELECT p.user.username, SUM(p.points) as totalPoints FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false GROUP BY p.user.id, p.user.username ORDER BY totalPoints DESC LIMIT 10")
    List<Object []> findTop10ByTotalPoints();

    @Query("SELECT w.user.username, COUNT(g) as wins FROM Game g JOIN g.winners w WHERE g.endDate IS NOT NULL AND g.cancelled = false GROUP BY w.user.id, w.user.username ORDER BY wins DESC LIMIT 10")
    List<Object []> findTop10ByWins();

    @Query("SELECT p.user.username, AVG(p.points) as averagePoints FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false GROUP BY p.user.id, p.user.username ORDER BY averagePoints DESC LIMIT 10")
    List<Object []> findTop10ByAveragePointsPerGame();

    @Query("SELECT c.user.username, COUNT(v) as trapVotesReceived FROM Card c JOIN c.votes v JOIN c.turn t WHERE t.storyteller.id != c.user.id GROUP BY c.user.id, c.user.username ORDER BY trapVotesReceived DESC LIMIT 10")
    List<Object []> findTop10ByTrapVotesReceived();

    //ranking by win rate also done in service

    //ranking by deception rate also done in service

    //helpers for win rate ranking
    @Query("SELECT w.user.username, COUNT(g) as wins FROM Game g JOIN g.winners w WHERE g.endDate IS NOT NULL AND g.cancelled = false GROUP BY w.user.id, w.user.username")
    List<Object []> findAllWins();

    @Query("SELECT p.user.username, COUNT(g) as gamesPlayed FROM Game g JOIN g.players p WHERE g.endDate IS NOT NULL AND g.cancelled = false GROUP BY p.user.id, p.user.username")
    List<Object []> findAllGamesPlayed();

    //helpers for deception rate ranking
    @Query("SELECT c.user.username, COUNT(v) FROM Card c JOIN c.votes v JOIN c.turn t WHERE t.storyteller.id != c.user.id GROUP BY c.user.id, c.user.username")
    List<Object []> findAllTrapVotes();

    @Query("SELECT c.user.username, COUNT(c) FROM Card c JOIN c.turn t WHERE t.storyteller.id != c.user.id GROUP BY c.user.id, c.user.username")
    List<Object []> findAllDeceptionOpportunities();

}
