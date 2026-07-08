package ro.betrio.backend.service.prediction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.MatchPredictionDto;

class ProbabilityEngineServiceTest {

    private final ProbabilityEngineService probabilityEngineService = new ProbabilityEngineService();

    @Test
    void oneXTwoProbabilitiesSumToOneForHighLambdas() {
        MatchPredictionDto prediction = probabilityEngineService.predict(features(3.5, 3.5));

        double sum = prediction.homeWinProbability()
                + prediction.drawProbability()
                + prediction.awayWinProbability();

        assertThat(sum).isBetween(0.9998, 1.0002);
    }

    @Test
    void overUnderProbabilitiesAreComplementary() {
        MatchPredictionDto prediction = probabilityEngineService.predict(features(1.5, 1.2));

        double sum = prediction.over25Probability() + prediction.under25Probability();

        assertThat(sum).isBetween(0.9998, 1.0002);
    }

    @Test
    void bttsProbabilitiesAreComplementary() {
        MatchPredictionDto prediction = probabilityEngineService.predict(features(1.5, 1.2));

        double sum = prediction.bttsYesProbability() + prediction.bttsNoProbability();

        assertThat(sum).isBetween(0.9998, 1.0002);
    }

    @Test
    void topExactScoresAreReturnedWithValidProbabilities() {
        MatchPredictionDto prediction = probabilityEngineService.predict(features(1.5, 1.2));

        assertThat(prediction.topExactScores()).hasSize(5);

        assertThat(prediction.topExactScores())
                .allSatisfy(score -> assertThat(score.probability()).isBetween(0.0, 1.0));
    }

    private MatchFeatureSnapshotDto features(double expectedHomeGoals, double expectedAwayGoals) {
        return new MatchFeatureSnapshotDto(
                1L,
                "Home",
                "Away",
                "2026-07-09T20:00:00+03:00",
                null,
                null,
                null,
                expectedHomeGoals,
                expectedAwayGoals
        );
    }
}