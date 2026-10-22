package es.us.dp1.lx_xy_24_25.dixit.statistics;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.GlobalStatisticsDTO;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.RankingDTO;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.UserStatisticsDTO;


@RestController
@RequestMapping("/api/v1/stats")
public class StatisticsRestController {
    
    private final StatisticsService statisticsService;

    public StatisticsRestController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/global")
    public GlobalStatisticsDTO getGlobalStatistics() {
        return statisticsService.getGlobalStatistics();
    }

    @GetMapping("/user/{userId}")
    public UserStatisticsDTO getUserStatistics(@PathVariable Integer userId) {
        return statisticsService.getUserStatistics(userId);
    }

    @GetMapping("/rankings/points")
    public List<RankingDTO> getRankingByPoints() {
        return statisticsService.getRankingByPoints();
    }

    @GetMapping("/rankings/avg-points")
    public List<RankingDTO> getRankingByAvgPoints() {
        return statisticsService.getRankingByAveragePointsPerGame();
    }

    @GetMapping("/rankings/trap-votes")
    public List<RankingDTO> getRankingByTrapVotesReceived() {
        return statisticsService.getRankingByTrapVotesReceived();
    }
    
    @GetMapping("/rankings/wins")
    public List<RankingDTO> getRankingByWins() {
        return statisticsService.getRankingByWins();
    }

    @GetMapping("/rankings/win-rate")
    public List<RankingDTO> getRankingByWinRate() {
        return statisticsService.getRankingByWinRate();
    }

    @GetMapping("/rankings/deception-rate")
    public List<RankingDTO> getRankingByDeceptionRate() {
        return statisticsService.getRankingByDeceptionRate();
    }

}
