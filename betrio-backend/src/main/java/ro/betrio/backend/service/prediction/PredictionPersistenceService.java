package ro.betrio.backend.service.prediction;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.ExactScoreDto;
import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.MatchPredictionDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.PredictionExactScore;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.PredictionExactScoreRepository;
import ro.betrio.backend.repository.PredictionRunRepository;
import ro.betrio.backend.service.analysis.FeatureBuilderService;

@Service
public class PredictionPersistenceService {

    private static final String MODEL_VERSION =
            "poisson-v2-context";

    private static final double CHANGE_EPSILON = 0.000000001;

    private final FixtureRepository fixtureRepository;
    private final FeatureBuilderService featureBuilderService;
    private final ProbabilityEngineService probabilityEngineService;
    private final PredictionRunRepository predictionRunRepository;
    private final PredictionExactScoreRepository
            predictionExactScoreRepository;

    public PredictionPersistenceService(
            FixtureRepository fixtureRepository,
            FeatureBuilderService featureBuilderService,
            ProbabilityEngineService probabilityEngineService,
            PredictionRunRepository predictionRunRepository,
            PredictionExactScoreRepository predictionExactScoreRepository) {

        this.fixtureRepository = fixtureRepository;
        this.featureBuilderService = featureBuilderService;
        this.probabilityEngineService = probabilityEngineService;
        this.predictionRunRepository = predictionRunRepository;
        this.predictionExactScoreRepository =
                predictionExactScoreRepository;
    }

    @Transactional
    public Long generateAndStorePrediction(Long fixtureId) {
        PredictionCalculation calculation =
                calculatePrediction(fixtureId);

        return storePrediction(calculation);
    }

    @Transactional
    public Optional<Long> generateAndStorePredictionIfChanged(
            Long fixtureId) {

        PredictionCalculation calculation =
                calculatePrediction(fixtureId);

        Optional<PredictionRun> latestPrediction =
                predictionRunRepository
                        .findTopByFixtureIdOrderByGeneratedAtDesc(
                                fixtureId);

        if (latestPrediction.isPresent()
                && isSamePrediction(
                        latestPrediction.get(),
                        calculation)) {

            return Optional.empty();
        }

        Long runId = storePrediction(calculation);
        return Optional.of(runId);
    }

    private PredictionCalculation calculatePrediction(
            Long fixtureId) {

        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Fixture not found: " + fixtureId
                        )
                );

        MatchFeatureSnapshotDto features =
                featureBuilderService.buildForFixture(fixtureId);

        MatchPredictionDto prediction =
                probabilityEngineService.predict(features);

        return new PredictionCalculation(
                fixture,
                features,
                prediction
        );
    }

    private Long storePrediction(
            PredictionCalculation calculation) {

        Fixture fixture = calculation.fixture();
        MatchFeatureSnapshotDto features =
                calculation.features();
        MatchPredictionDto prediction =
                calculation.prediction();

        PredictionRun run = new PredictionRun();

        run.setFixture(fixture);
        run.setModelVersion(MODEL_VERSION);

        run.setExpectedHomeGoals(
                features.expectedHomeGoals());

        run.setExpectedAwayGoals(
                features.expectedAwayGoals());

        run.setHomeWinProbability(
                prediction.homeWinProbability());

        run.setDrawProbability(
                prediction.drawProbability());

        run.setAwayWinProbability(
                prediction.awayWinProbability());

        run.setOver25Probability(
                prediction.over25Probability());

        run.setUnder25Probability(
                prediction.under25Probability());

        run.setBttsYesProbability(
                prediction.bttsYesProbability());

        run.setBttsNoProbability(
                prediction.bttsNoProbability());

        run = predictionRunRepository.save(run);

        for (ExactScoreDto exactScore :
                prediction.topExactScores()) {

            PredictionExactScore row =
                    new PredictionExactScore();

            row.setPredictionRun(run);
            row.setHomeGoals(exactScore.homeGoals());
            row.setAwayGoals(exactScore.awayGoals());
            row.setProbability(exactScore.probability());

            predictionExactScoreRepository.save(row);
        }

        return run.getId();
    }

    private boolean isSamePrediction(
            PredictionRun latest,
            PredictionCalculation calculation) {

        MatchFeatureSnapshotDto features =
                calculation.features();

        MatchPredictionDto prediction =
                calculation.prediction();

        return MODEL_VERSION.equals(latest.getModelVersion())
                && sameNumber(
                        latest.getExpectedHomeGoals(),
                        features.expectedHomeGoals())
                && sameNumber(
                        latest.getExpectedAwayGoals(),
                        features.expectedAwayGoals())
                && sameNumber(
                        latest.getHomeWinProbability(),
                        prediction.homeWinProbability())
                && sameNumber(
                        latest.getDrawProbability(),
                        prediction.drawProbability())
                && sameNumber(
                        latest.getAwayWinProbability(),
                        prediction.awayWinProbability())
                && sameNumber(
                        latest.getOver25Probability(),
                        prediction.over25Probability())
                && sameNumber(
                        latest.getUnder25Probability(),
                        prediction.under25Probability())
                && sameNumber(
                        latest.getBttsYesProbability(),
                        prediction.bttsYesProbability())
                && sameNumber(
                        latest.getBttsNoProbability(),
                        prediction.bttsNoProbability());
    }

    private boolean sameNumber(
            Double storedValue,
            double calculatedValue) {

        if (storedValue == null
                || !Double.isFinite(storedValue)
                || !Double.isFinite(calculatedValue)) {
            return false;
        }

        return Math.abs(
                storedValue - calculatedValue
        ) <= CHANGE_EPSILON;
    }

    private record PredictionCalculation(
            Fixture fixture,
            MatchFeatureSnapshotDto features,
            MatchPredictionDto prediction
    ) {
    }
}