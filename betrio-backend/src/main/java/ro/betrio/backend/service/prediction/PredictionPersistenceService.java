package ro.betrio.backend.service.prediction;

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

    private static final String MODEL_VERSION = "v1-baseline-poisson";

    private final FixtureRepository fixtureRepository;
    private final FeatureBuilderService featureBuilderService;
    private final ProbabilityEngineService probabilityEngineService;
    private final PredictionRunRepository predictionRunRepository;
    private final PredictionExactScoreRepository predictionExactScoreRepository;

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
        this.predictionExactScoreRepository = predictionExactScoreRepository;
    }

    @Transactional
    public Long generateAndStorePrediction(Long fixtureId) {
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        MatchFeatureSnapshotDto features = featureBuilderService.buildForFixture(fixtureId);
        MatchPredictionDto prediction = probabilityEngineService.predictForFixture(fixtureId);

        PredictionRun run = new PredictionRun();
        run.setFixture(fixture);
        run.setModelVersion(MODEL_VERSION);
        run.setExpectedHomeGoals(features.expectedHomeGoals());
        run.setExpectedAwayGoals(features.expectedAwayGoals());
        run.setHomeWinProbability(prediction.homeWinProbability());
        run.setDrawProbability(prediction.drawProbability());
        run.setAwayWinProbability(prediction.awayWinProbability());
        run.setOver25Probability(prediction.over25Probability());
        run.setUnder25Probability(prediction.under25Probability());
        run.setBttsYesProbability(prediction.bttsYesProbability());
        run.setBttsNoProbability(prediction.bttsNoProbability());

        run = predictionRunRepository.save(run);

        for (ExactScoreDto exactScore : prediction.topExactScores()) {
            PredictionExactScore row = new PredictionExactScore();
            row.setPredictionRun(run);
            row.setHomeGoals(exactScore.homeGoals());
            row.setAwayGoals(exactScore.awayGoals());
            row.setProbability(exactScore.probability());
            predictionExactScoreRepository.save(row);
        }

        return run.getId();
    }
}