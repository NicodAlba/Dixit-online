package es.us.dp1.lx_xy_24_25.dixit.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GlobalStatisticsDTO {
    
    //games
    private Long totalGamesPlayed;
    private Double averageGamesPerUser;
    private Long maxGamesByUser;
    private Long minGamesByUser;
    //duration
    private Long totalDuration;
    private Double averageDuration;
    private Long maxDuration;
    private Long minDuration;
    //players
    private Long totalUsersThatHavePlayed;
    private Double averagePlayersPerGame;
    private Long maxPlayersInGame;
    private Long minPlayersInGame;
    private Long currentUsersPlaying;


}
