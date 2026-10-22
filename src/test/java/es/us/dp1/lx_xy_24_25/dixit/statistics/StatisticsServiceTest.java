package es.us.dp1.lx_xy_24_25.dixit.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import es.us.dp1.lx_xy_24_25.dixit.card.Image;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.GlobalStatisticsDTO;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.RankingDTO;
import es.us.dp1.lx_xy_24_25.dixit.statistics.dto.UserStatisticsDTO;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

	@Mock
	private StatisticsRepository statisticsRepository;

	private StatisticsService statisticsService;

	private final Integer fixtureUserId = 18;

	@BeforeEach
	void setUp() {
		this.statisticsService = new StatisticsService(statisticsRepository);
	}

	@Test
	void shouldBuildUserStatisticsFromRepositoryValues() {
		when(statisticsRepository.countCompletedGamesByUserId(fixtureUserId)).thenReturn(4L);
		when(statisticsRepository.countWinsByUserId(fixtureUserId)).thenReturn(2L);
		when(statisticsRepository.totalPointsByUserId(fixtureUserId)).thenReturn(120L);
		when(statisticsRepository.averagePointsPerGameByUserId(fixtureUserId)).thenReturn(30.0);
		when(statisticsRepository.findMostPlayedCardByUserId(fixtureUserId)).thenAnswer(invocation -> {
			Image card = new Image();
			ReflectionTestUtils.setField(card, "url", "/Cards/7.png");
			return card;
		});
		when(statisticsRepository.totalTrapVotesReceivedByUserId(fixtureUserId)).thenReturn(3L);
		when(statisticsRepository.totalTurnsAsNormalPlayerByUserId(fixtureUserId)).thenReturn(6L);
		when(statisticsRepository.totalDurationByUserId(fixtureUserId)).thenReturn(90L);
		when(statisticsRepository.averageDurationByUserId(fixtureUserId)).thenReturn(22.5);
		when(statisticsRepository.maximumDurationByUserId(fixtureUserId)).thenReturn(25L);
		when(statisticsRepository.minimumDurationByUserId(fixtureUserId)).thenReturn(20L);

		UserStatisticsDTO stats = statisticsService.getUserStatistics(fixtureUserId);

		assertEquals(4L, stats.getGamesPlayed());
		assertEquals(2L, stats.getGamesWon());
		assertEquals(120L, stats.getTotalPoints());
		assertEquals(0.5, stats.getWinRate());
		assertEquals(30.0, stats.getAveragePointsPerGame());
		assertEquals("/Cards/7.png", stats.getFavouriteCard().getUrl());
		assertEquals(3L, stats.getTrapVotesReceived());
		assertEquals(0.5, stats.getDeceptionRate());
		assertEquals(90L, stats.getTotalDuration());
		assertEquals(22.5, stats.getAverageDuration());
		assertEquals(25L, stats.getMaxDuration());
		assertEquals(20L, stats.getMinDuration());
	}

	@Test
	void shouldHandleNullRepositoryValuesForUserStatistics() {
		when(statisticsRepository.countCompletedGamesByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.countWinsByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.totalPointsByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.averagePointsPerGameByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.findMostPlayedCardByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.totalTrapVotesReceivedByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.totalTurnsAsNormalPlayerByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.totalDurationByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.averageDurationByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.maximumDurationByUserId(fixtureUserId)).thenReturn(null);
		when(statisticsRepository.minimumDurationByUserId(fixtureUserId)).thenReturn(null);

		UserStatisticsDTO stats = statisticsService.getUserStatistics(fixtureUserId);

		assertEquals(0L, stats.getGamesPlayed());
		assertEquals(0L, stats.getGamesWon());
		assertEquals(0L, stats.getTotalPoints());
		assertEquals(0.0, stats.getWinRate());
		assertEquals(0.0, stats.getAveragePointsPerGame());
		assertEquals(null, stats.getFavouriteCard());
		assertEquals(0L, stats.getTrapVotesReceived());
		assertEquals(0.0, stats.getDeceptionRate());
		assertEquals(0L, stats.getTotalDuration());
		assertEquals(0.0, stats.getAverageDuration());
		assertEquals(0L, stats.getMaxDuration());
		assertEquals(0L, stats.getMinDuration());
	}

	@Test
	void shouldAssembleGlobalStatisticsFromRepository() {
		when(statisticsRepository.countCompletedGames()).thenReturn(10L);
		when(statisticsRepository.averageGamesPerUser()).thenReturn(2.0);
		when(statisticsRepository.maximumGamesByUser()).thenReturn(5L);
		when(statisticsRepository.minimumGamesByUser()).thenReturn(1L);
		when(statisticsRepository.totalDuration()).thenReturn(300L);
		when(statisticsRepository.averageDuration()).thenReturn(30.0);
		when(statisticsRepository.maximumDuration()).thenReturn(40L);
		when(statisticsRepository.minimumDuration()).thenReturn(10L);
		when(statisticsRepository.totalUsersThatHavePlayed()).thenReturn(6L);
		when(statisticsRepository.averagePlayersPerGame()).thenReturn(3.0);
		when(statisticsRepository.maximumPlayersInGame()).thenReturn(4L);
		when(statisticsRepository.minimumPlayersInGame()).thenReturn(3L);
		when(statisticsRepository.currentUsersPlaying()).thenReturn(2L);

		GlobalStatisticsDTO stats = statisticsService.getGlobalStatistics();

		assertEquals(10L, stats.getTotalGamesPlayed());
		assertEquals(2.0, stats.getAverageGamesPerUser());
		assertEquals(5L, stats.getMaxGamesByUser());
		assertEquals(1L, stats.getMinGamesByUser());
		assertEquals(300L, stats.getTotalDuration());
		assertEquals(30.0, stats.getAverageDuration());
		assertEquals(40L, stats.getMaxDuration());
		assertEquals(10L, stats.getMinDuration());
		assertEquals(6L, stats.getTotalUsersThatHavePlayed());
		assertEquals(3.0, stats.getAveragePlayersPerGame());
		assertEquals(4L, stats.getMaxPlayersInGame());
		assertEquals(3L, stats.getMinPlayersInGame());
		assertEquals(2L, stats.getCurrentUsersPlaying());
	}

	@Test
	void shouldHandleNullRepositoryValuesForGlobalStatistics() {
		when(statisticsRepository.countCompletedGames()).thenReturn(null);
		when(statisticsRepository.averageGamesPerUser()).thenReturn(null);
		when(statisticsRepository.maximumGamesByUser()).thenReturn(null);
		when(statisticsRepository.minimumGamesByUser()).thenReturn(null);
		when(statisticsRepository.totalDuration()).thenReturn(null);
		when(statisticsRepository.averageDuration()).thenReturn(null);
		when(statisticsRepository.maximumDuration()).thenReturn(null);
		when(statisticsRepository.minimumDuration()).thenReturn(null);
		when(statisticsRepository.totalUsersThatHavePlayed()).thenReturn(null);
		when(statisticsRepository.averagePlayersPerGame()).thenReturn(null);
		when(statisticsRepository.maximumPlayersInGame()).thenReturn(null);
		when(statisticsRepository.minimumPlayersInGame()).thenReturn(null);
		when(statisticsRepository.currentUsersPlaying()).thenReturn(null);

		GlobalStatisticsDTO stats = statisticsService.getGlobalStatistics();

		assertEquals(0L, stats.getTotalGamesPlayed());
		assertEquals(0.0, stats.getAverageGamesPerUser());
		assertEquals(0L, stats.getMaxGamesByUser());
		assertEquals(0L, stats.getMinGamesByUser());
		assertEquals(0L, stats.getTotalDuration());
		assertEquals(0.0, stats.getAverageDuration());
		assertEquals(0L, stats.getMaxDuration());
		assertEquals(0L, stats.getMinDuration());
		assertEquals(0L, stats.getTotalUsersThatHavePlayed());
		assertEquals(0.0, stats.getAveragePlayersPerGame());
		assertEquals(0L, stats.getMaxPlayersInGame());
		assertEquals(0L, stats.getMinPlayersInGame());
		assertEquals(0L, stats.getCurrentUsersPlaying());
	}

    @Test
	void shouldReturnEmptyListForEmptyDirectRankings() {
		when(statisticsRepository.findTop10ByTotalPoints()).thenReturn(List.of());
		when(statisticsRepository.findTop10ByWins()).thenReturn(List.of());
		when(statisticsRepository.findTop10ByAveragePointsPerGame()).thenReturn(List.of());
		when(statisticsRepository.findTop10ByTrapVotesReceived()).thenReturn(List.of());
		when(statisticsRepository.findAllGamesPlayed()).thenReturn(List.of());
		when(statisticsRepository.findAllWins()).thenReturn(List.of());
		when(statisticsRepository.findAllDeceptionOpportunities()).thenReturn(List.of());
		when(statisticsRepository.findAllTrapVotes()).thenReturn(List.of());

		assertTrue(statisticsService.getRankingByPoints().isEmpty());
		assertTrue(statisticsService.getRankingByWins().isEmpty());
		assertTrue(statisticsService.getRankingByAveragePointsPerGame().isEmpty());
		assertTrue(statisticsService.getRankingByTrapVotesReceived().isEmpty());
		assertTrue(statisticsService.getRankingByWinRate().isEmpty());
		assertTrue(statisticsService.getRankingByDeceptionRate().isEmpty());
	}

	@Test
	void shouldComputeRankingByPoints() {
		when(statisticsRepository.findTop10ByTotalPoints()).thenReturn(List.of(
			new Object[] { "alice", 150L },
			new Object[] { "bob", 120L }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByPoints();

		assertEquals(2, ranking.size());
		assertEquals("alice", ranking.get(0).getUsername());
		assertEquals(150L, ranking.get(0).getValue());
		assertEquals("bob", ranking.get(1).getUsername());
		assertEquals(120L, ranking.get(1).getValue());
	}

	@Test
	void shouldSortRankingByWins() {
		when(statisticsRepository.findTop10ByWins()).thenReturn(List.of(
			new Object[] { "alice", 12L },
			new Object[] { "bob", 10L }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByWins();

		assertEquals(2, ranking.size());
		assertEquals("alice", ranking.get(0).getUsername());
		assertEquals(12L, ranking.get(0).getValue());
		assertEquals("bob", ranking.get(1).getUsername());
		assertEquals(10L, ranking.get(1).getValue());
	}

	@Test
	void shouldSortRankingByAveragePoints() {
		when(statisticsRepository.findTop10ByAveragePointsPerGame()).thenReturn(List.of(
			new Object[] { "alice", 18.5 },
			new Object[] { "bob", 16.0 }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByAveragePointsPerGame();

		assertEquals(2, ranking.size());
		assertEquals("alice", ranking.get(0).getUsername());
		assertEquals(18.5, ranking.get(0).getValue());
		assertEquals("bob", ranking.get(1).getUsername());
		assertEquals(16.0, ranking.get(1).getValue());
	}

	@Test
	void shouldSortRankingByTrapVotesReceived() {
		when(statisticsRepository.findTop10ByTrapVotesReceived()).thenReturn(List.of(
			new Object[] { "alice", 7L },
			new Object[] { "bob", 5L }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByTrapVotesReceived();

		assertEquals(2, ranking.size());
		assertEquals("alice", ranking.get(0).getUsername());
		assertEquals(7L, ranking.get(0).getValue());
		assertEquals("bob", ranking.get(1).getUsername());
		assertEquals(5L, ranking.get(1).getValue());
	}

	@Test
	void shouldSortRankingByWinRate() {
		when(statisticsRepository.findAllGamesPlayed()).thenReturn(List.of(
			new Object[] { "alice", 4L },
			new Object[] { "bob", 2L }
		));
		when(statisticsRepository.findAllWins()).thenReturn(List.of(
			new Object[] { "bob", 2L },
			new Object[] { "alice", 1L }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByWinRate();

		assertEquals("bob", ranking.get(0).getUsername());
		assertEquals(1.0, ranking.get(0).getValue());
		assertEquals("alice", ranking.get(1).getUsername());
		assertEquals(0.25, ranking.get(1).getValue());
	}

	@Test
	void shouldSortRankingByDeceptionRate() {
		when(statisticsRepository.findAllDeceptionOpportunities()).thenReturn(List.of(
			new Object[] { "alice", 3L },
			new Object[] { "bob", 5L }
		));
		when(statisticsRepository.findAllTrapVotes()).thenReturn(List.of(
			new Object[] { "alice", 1L },
			new Object[] { "bob", 4L }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByDeceptionRate();

		assertEquals("bob", ranking.get(0).getUsername());
		assertEquals(0.8, ranking.get(0).getValue());
		assertEquals("alice", ranking.get(1).getUsername());
		assertEquals(0.3333333333333333, ranking.get(1).getValue());
	}

	@Test
	void shouldTreatZeroGamesAsZeroWinRate() {
		when(statisticsRepository.countCompletedGamesByUserId(fixtureUserId)).thenReturn(0L);
		when(statisticsRepository.countWinsByUserId(fixtureUserId)).thenReturn(1L);

		UserStatisticsDTO stats = statisticsService.getUserStatistics(fixtureUserId);

		assertEquals(0.0, stats.getWinRate());
	}

	@Test
	void shouldLimitWinRateRankingToTop10()
	{
		when(statisticsRepository.findAllGamesPlayed()).thenReturn(List.of(
			new Object[] { "user1", 4L },
			new Object[] { "user2", 4L },
			new Object[] { "user3", 4L },
			new Object[] { "user4", 4L },
			new Object[] { "user5", 4L },
			new Object[] { "user6", 4L },
			new Object[] { "user7", 4L },
			new Object[] { "user8", 4L },
			new Object[] { "user9", 4L },
			new Object[] { "user10", 4L },
			new Object[] { "user11", 4L },
			new Object[] { "user12", 4L }
		));
		when(statisticsRepository.findAllWins()).thenReturn(List.of(
			new Object[] { "user1", 4L },
			new Object[] { "user2", 3L },
			new Object[] { "user3", 3L },
			new Object[] { "user4", 3L },
			new Object[] { "user5", 2L },
			new Object[] { "user6", 2L },
			new Object[] { "user7", 2L },
			new Object[] { "user8", 2L },
			new Object[] { "user9", 1L },
			new Object[] { "user10", 1L },
			new Object[] { "user11", 1L },
			new Object[] { "user12", 0L }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByWinRate();

		assertEquals(10, ranking.size());
		assertEquals("user10", ranking.get(9).getUsername());
	}

	@Test
	void shouldLimitDeceptionRateRankingToTop10()
	{
		when(statisticsRepository.findAllDeceptionOpportunities()).thenReturn(List.of(
			new Object[] { "user1", 5L },
			new Object[] { "user2", 5L },
			new Object[] { "user3", 5L },
			new Object[] { "user4", 5L },
			new Object[] { "user5", 5L },
			new Object[] { "user6", 5L },
			new Object[] { "user7", 5L },
			new Object[] { "user8", 5L },
			new Object[] { "user9", 5L },
			new Object[] { "user10", 5L },
			new Object[] { "user11", 5L },
			new Object[] { "user12", 5L }
		));
		when(statisticsRepository.findAllTrapVotes()).thenReturn(List.of(
			new Object[] { "user1", 5L },
			new Object[] { "user2", 4L },
			new Object[] { "user3", 4L },
			new Object[] { "user4", 4L },
			new Object[] { "user5", 3L },
			new Object[] { "user6", 3L },
			new Object[] { "user7", 3L },
			new Object[] { "user8", 3L },
			new Object[] { "user9", 2L },
			new Object[] { "user10", 2L },
			new Object[] { "user11", 1L },
			new Object[] { "user12", 0L }
		));

		List<RankingDTO> ranking = statisticsService.getRankingByDeceptionRate();

		assertEquals(10, ranking.size());
		assertEquals("user10", ranking.get(9).getUsername());
	}
}

