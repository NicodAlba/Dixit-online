package es.us.dp1.lx_xy_24_25.dixit.statistics;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.dp1.lx_xy_24_25.dixit.card.Image;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.GlobalStatisticsDTO;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.RankingDTO;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.UserStatisticsDTO;


@Service
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;

    public StatisticsService(StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    //helper methods to prevent from NullPointerExceptions
    private Long safeLong(Long value) {
        return value != null ? value : 0L;
    }
    private Double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private Double calculateRatio(Long numerator, Long denominator) {
        if (denominator == null || denominator == 0) {
            return 0.0;
        }
        return (numerator != null ? numerator.doubleValue() : 0.0) / denominator;
    }

    //-----------------------User statistics------------------------

    @Transactional(readOnly = true)
    public UserStatisticsDTO getUserStatistics(Integer userId) {
        
        Long gamesPlayed = safeLong(statisticsRepository.countCompletedGamesByUserId(userId));
        Long gamesWon = safeLong(statisticsRepository.countWinsByUserId(userId));
        Long totalPoints = safeLong(statisticsRepository.totalPointsByUserId(userId));
        Double winRate = calculateRatio(gamesWon, gamesPlayed);
        Double averagePointsPerGame = safeDouble(statisticsRepository.averagePointsPerGameByUserId(userId));
        Image favouriteCard = statisticsRepository.findMostPlayedCardByUserId(userId);
        Long trapVotesReceived = safeLong(statisticsRepository.totalTrapVotesReceivedByUserId(userId));
        Long totalTurnsAsNormalPlayer = safeLong(statisticsRepository.totalTurnsAsNormalPlayerByUserId(userId));
        Double deceptionRate = calculateRatio(trapVotesReceived, totalTurnsAsNormalPlayer);
        Long totalDuration = safeLong(statisticsRepository.totalDurationByUserId(userId));
        Double averageDuration = safeDouble(statisticsRepository.averageDurationByUserId(userId));
        Long maxDuration = safeLong(statisticsRepository.maximumDurationByUserId(userId));
        Long minDuration = safeLong(statisticsRepository.minimumDurationByUserId(userId));


        return new UserStatisticsDTO(
            gamesPlayed,
            gamesWon,
            totalPoints,
            winRate,
            averagePointsPerGame,
            favouriteCard,
            trapVotesReceived,
            deceptionRate,
            totalDuration,
            averageDuration,
            maxDuration,
            minDuration
        );

        
    }

    //-----------------------Global statistics------------------------

    @Transactional(readOnly = true)
    public GlobalStatisticsDTO getGlobalStatistics() {
        Long totalGames = safeLong(statisticsRepository.countCompletedGames());
        Double averageGamesPerUser = safeDouble(statisticsRepository.averageGamesPerUser());
        Long maximumGamesByUser = safeLong(statisticsRepository.maximumGamesByUser());
        Long minimumGamesByUser = safeLong(statisticsRepository.minimumGamesByUser());
        Long totalDuration = safeLong(statisticsRepository.totalDuration());
        Double averageDuration = safeDouble(statisticsRepository.averageDuration());
        Long maximumDuration = safeLong(statisticsRepository.maximumDuration());
        Long minimumDuration = safeLong(statisticsRepository.minimumDuration());
        Long totalUsersThatHavePlayed = safeLong(statisticsRepository.totalUsersThatHavePlayed());
        Double averagePlayersPerGame = safeDouble(statisticsRepository.averagePlayersPerGame());
        Long maximumPlayersInGame = safeLong(statisticsRepository.maximumPlayersInGame());
        Long minimumPlayersInGame = safeLong(statisticsRepository.minimumPlayersInGame());
        Long currentUsersPlaying = safeLong(statisticsRepository.currentUsersPlaying());

        return new GlobalStatisticsDTO(
            totalGames,
            averageGamesPerUser,
            maximumGamesByUser,
            minimumGamesByUser,
            totalDuration,
            averageDuration,
            maximumDuration,
            minimumDuration,
            totalUsersThatHavePlayed,
            averagePlayersPerGame,
            maximumPlayersInGame,
            minimumPlayersInGame,
            currentUsersPlaying
        );
    }

    //-----------------------rankings------------------------

    @Transactional(readOnly = true)
    public List<RankingDTO> getRankingByPoints() {
        return statisticsRepository.findTop10ByTotalPoints().stream()
        .map(row -> new RankingDTO(
            (String) row[0],  //username
            (Number) row[1]     //totalPoints
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RankingDTO> getRankingByWins() {
        return statisticsRepository.findTop10ByWins().stream()
        .map(row -> new RankingDTO(
            (String) row[0],  //username
            (Number) row[1]     //wins
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RankingDTO> getRankingByAveragePointsPerGame() {
        return statisticsRepository.findTop10ByAveragePointsPerGame().stream()
        .map(row -> new RankingDTO(
            (String) row[0],  //username
            (Number) row[1]     //averagePointsPerGame
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RankingDTO> getRankingByTrapVotesReceived() {
        return statisticsRepository.findTop10ByTrapVotesReceived().stream()
        .map(row -> new RankingDTO(
            (String) row[0],  //username
            (Number) row[1]     //trapVotesReceived
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RankingDTO> getRankingByWinRate() {
        Map<String, Long> gamesPlayedMap = statisticsRepository.findAllGamesPlayed().stream()
        .collect(Collectors.toMap(
            row -> (String) row[0], //username
            row -> safeLong((Long) row[1]) //gamesPlayed
        ));
        List<Object []> winsData = statisticsRepository.findAllWins();
        return winsData.stream()
        .map(row -> {
            String username = (String) row[0];
            Long wins = safeLong((Long) row[1]);
            Long gamesPlayed = gamesPlayedMap.getOrDefault(username, 0L);
            Double winRate = calculateRatio(wins, gamesPlayed);
            return new RankingDTO(
                username,
                winRate
            );
        }).sorted(Comparator.comparing(r -> r.getValue().doubleValue(), Comparator.reverseOrder())).limit(10)
        .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RankingDTO> getRankingByDeceptionRate() {
        Map<String, Long> opportunitiesMap = statisticsRepository.findAllDeceptionOpportunities().stream()
        .collect(Collectors.toMap(
            row -> (String) row[0], //username
            row -> safeLong((Long) row[1]) //totalTurnsAsNormalPlayer
        ));

        List<Object []> votesData = statisticsRepository.findAllTrapVotes();
        return votesData.stream()
        .map(row -> {
            String username = (String) row[0];
            Long trapVotes = safeLong((Long) row[1]);
            Long opportunities = opportunitiesMap.getOrDefault(username, 0L);
            Double deceptionRate = calculateRatio(trapVotes, opportunities);
            return new RankingDTO(
                username,
                deceptionRate
            );
        }).sorted(Comparator.comparing(r -> r.getValue().doubleValue(), Comparator.reverseOrder())).limit(10)
        .collect(Collectors.toList());  
    }
        


    
}
