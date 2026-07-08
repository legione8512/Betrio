package ro.betrio.backend.service.prediction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import org.springframework.data.domain.PageRequest;

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
        MatchPredictionDto prediction =
                probabilityEngineService.predict(features);
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
    @Transactional
    public String generateAndStorePredictionsForUpcomingCompetition(Long competitionId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));

        List<Fixture> fixtures = fixtureRepository.findUpcomingWithTeamsByCompetition(
                competitionId,
                PageRequest.of(0, safeLimit)
        );

        int created = 0;
        int skippedExisting = 0;

        StringBuilder createdFixtureIds = new StringBuilder();
        StringBuilder skippedFixtureIds = new StringBuilder();

        for (Fixture fixture : fixtures) {
            boolean alreadyExists = predictionRunRepository
                    .findTopByFixtureIdOrderByGeneratedAtDesc(fixture.getId())
                    .isPresent();

            if (alreadyExists) {
                skippedExisting++;
                if (skippedFixtureIds.length() > 0) {
                    skippedFixtureIds.append(",");
                }
                skippedFixtureIds.append(fixture.getId());
                continue;
            }

            generateAndStorePrediction(fixture.getId());
            created++;

            if (createdFixtureIds.length() > 0) {
                createdFixtureIds.append(",");
            }
            createdFixtureIds.append(fixture.getId());
        }

        return "Batch prediction capture completed for competitionId=" + competitionId
                + ". fixturesFound=" + fixtures.size()
                + ", created=" + created
                + ", skippedExisting=" + skippedExisting
                + ", createdFixtureIds=[" + createdFixtureIds + "]"
                + ", skippedFixtureIds=[" + skippedFixtureIds + "]";
    }
}