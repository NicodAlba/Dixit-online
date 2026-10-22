package es.us.dp1.lx_xy_24_25.dixit.statistics.dto;

import es.us.dp1.lx_xy_24_25.dixit.card.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserStatisticsDTO {
    
    private Long gamesPlayed;
    private Long gamesWon;
    private Long totalPoints;
    private Double winRate;
    private Double averagePointsPerGame;
    private Image favouriteCard;
    private Long trapVotesReceived;
    private Double deceptionRate;
    //duration
    private Long totalDuration;
    private Double averageDuration;
    private Long maxDuration;
    private Long minDuration;
    
}
