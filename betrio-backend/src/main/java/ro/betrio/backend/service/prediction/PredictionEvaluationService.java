package ro.betrio.backend.service.prediction;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.PredictionEvaluationDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.OddsSnapshot;
import ro.betrio.backend.domain.entity.PredictionEvaluation;
import ro.betrio.backend.domain.entity.PredictionExactScore;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.OddsSnapshotRepository;
import ro.betrio.backend.repository.PredictionEvaluationRepository;
import ro.betrio.backend.repository.PredictionExactScoreRepository;
import ro.betrio.backend.repository.PredictionRunRepository;

@Service
public class PredictionEvaluationService {

    private final PredictionRunRepository predictionRunRepository;
    private final PredictionExactScoreRepository predictionExactScoreRepository;
    private final PredictionEvaluationRepository predictionEvaluationRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;

    public PredictionEvaluationService(
            PredictionRunRepository predictionRunRepository,
            PredictionExactScoreRepository predictionExactScoreRepository,
            PredictionEvaluationRepository predictionEvaluationRepository,
            OddsSnapshotRepository oddsSnapshotRepository) {
        this.predictionRunRepository = predictionRunRepository;
        this.predictionExactScoreRepository = predictionExactScoreRepository;
        this.predictionEvaluationRepository = predictionEvaluationRepository;
        this.oddsSnapshotRepository = oddsSnapshotRepository;
    }

    @Transactional
    public Long evaluatePredictionRun(Long predictionRunId) {
        PredictionRun run = predictionRunRepository.findById(predictionRunId)
                .orElseThrow(() -> new IllegalStateException("Prediction run not found: " + predictionRunId));

        Fixture fixture = run.getFixture();

        if (!isFinishedFixture(fixture)) {
            throw new IllegalStateException("Fixture is not finished yet, so the prediction cannot be evaluated.");
        }

        int actualHomeGoals = finalHomeGoals(fixture);
        int actualAwayGoals = finalAwayGoals(fixture);

        String actualResultCode = resultCode(actualHomeGoals, actualAwayGoals);
        String predictedResultCode = predictedResultCode(run);

        boolean actualOver25 = (actualHomeGoals + actualAwayGoals) >= 3;
        boolean actualBttsYes = actualHomeGoals >= 1 && actualAwayGoals >= 1;

        boolean predictedOver25 = run.getOver25Probability() >= run.getUnder25Probability();
        boolean predictedBttsYes = run.getBttsYesProbability() >= run.getBttsNoProbability();

        boolean hit1x2 = predictedResultCode.equals(actualResultCode);
        boolean hitOver25 = predictedOver25 == actualOver25;
        boolean hitBtts = predictedBttsYes == actualBttsYes;

        List<PredictionExactScore> exactScores = predictionExactScoreRepository
                .findByPredictionRunIdOrderByProbabilityDesc(run.getId());

        boolean topExactScoreHit = exactScores.stream().anyMatch(score ->
                score.getHomeGoals() == actualHomeGoals && score.getAwayGoals() == actualAwayGoals
        );

        double brierScore = brierScore1x2(run, actualResultCode);
        double logLoss = logLoss1x2(run, actualResultCode);

        MarketProbabilities market = extractMarketProbabilities(fixture);

        PredictionEvaluation evaluation = predictionEvaluationRepository
                .findByPredictionRunId(run.getId())
                .orElseGet(PredictionEvaluation::new);

        evaluation.setPredictionRun(run);
        evaluation.setActualHomeGoals(actualHomeGoals);
        evaluation.setActualAwayGoals(actualAwayGoals);
        evaluation.setPredictedResultCode(predictedResultCode);
        evaluation.setActualResultCode(actualResultCode);
        evaluation.setActualOver25(actualOver25);
        evaluation.setActualBttsYes(actualBttsYes);
        evaluation.setHit1x2(hit1x2);
        evaluation.setHitOver25(hitOver25);
        evaluation.setHitBtts(hitBtts);
        evaluation.setTopExactScoreHit(topExactScoreHit);
        evaluation.setBrierScore1x2(brierScore);
        evaluation.setLogLoss1x2(logLoss);

        if (market != null) {
            evaluation.setMarketHomeImpliedProbability(market.home());
            evaluation.setMarketDrawImpliedProbability(market.draw());
            evaluation.setMarketAwayImpliedProbability(market.away());

            evaluation.setModelEdgeHome(run.getHomeWinProbability() - market.home());
            evaluation.setModelEdgeDraw(run.getDrawProbability() - market.draw());
            evaluation.setModelEdgeAway(run.getAwayWinProbability() - market.away());
        } else {
            evaluation.setMarketHomeImpliedProbability(null);
            evaluation.setMarketDrawImpliedProbability(null);
            evaluation.setMarketAwayImpliedProbability(null);
            evaluation.setModelEdgeHome(null);
            evaluation.setModelEdgeDraw(null);
            evaluation.setModelEdgeAway(null);
        }

        evaluation = predictionEvaluationRepository.save(evaluation);
        return evaluation.getId();
    }

    @Transactional
    public Long evaluateLatestPredictionForFixture(Long fixtureId) {
        PredictionRun run = predictionRunRepository.findTopByFixtureIdOrderByGeneratedAtDesc(fixtureId)
                .orElseThrow(() -> new IllegalStateException("No prediction runs found for fixture id: " + fixtureId));

        return evaluatePredictionRun(run.getId());
    }

    @Transactional(readOnly = true)
    public PredictionEvaluationDto getEvaluationByPredictionRunId(Long predictionRunId) {
        PredictionEvaluation evaluation = predictionEvaluationRepository.findByPredictionRunId(predictionRunId)
                .orElseThrow(() -> new IllegalStateException("No evaluation found for prediction run id: " + predictionRunId));

        return new PredictionEvaluationDto(
                evaluation.getPredictionRun().getId(),
                evaluation.getPredictedResultCode(),
                evaluation.getActualResultCode(),
                evaluation.getActualHomeGoals(),
                evaluation.getActualAwayGoals(),
                evaluation.getActualOver25(),
                evaluation.getActualBttsYes(),
                evaluation.getHit1x2(),
                evaluation.getHitOver25(),
                evaluation.getHitBtts(),
                evaluation.getTopExactScoreHit(),
                evaluation.getBrierScore1x2(),
                evaluation.getLogLoss1x2(),
                evaluation.getMarketHomeImpliedProbability(),
                evaluation.getMarketDrawImpliedProbability(),
                evaluation.getMarketAwayImpliedProbability(),
                evaluation.getModelEdgeHome(),
                evaluation.getModelEdgeDraw(),
                evaluation.getModelEdgeAway()
        );
    }

    private boolean isFinishedFixture(Fixture fixture) {
        String status = fixture.getStatusShort();
        return "FT".equals(status) || "AET".equals(status) || "PEN".equals(status);
    }

    private int finalHomeGoals(Fixture fixture) {
        if (fixture.getFulltimeHomeGoals() != null) {
            return fixture.getFulltimeHomeGoals();
        }
        return fixture.getHomeGoals() != null ? fixture.getHomeGoals() : 0;
    }

    private int finalAwayGoals(Fixture fixture) {
        if (fixture.getFulltimeAwayGoals() != null) {
            return fixture.getFulltimeAwayGoals();
        }
        return fixture.getAwayGoals() != null ? fixture.getAwayGoals() : 0;
    }

    private String resultCode(int homeGoals, int awayGoals) {
        if (homeGoals > awayGoals) {
            return "HOME";
        }
        if (homeGoals < awayGoals) {
            return "AWAY";
        }
        return "DRAW";
    }

    private String predictedResultCode(PredictionRun run) {
        double home = run.getHomeWinProbability();
        double draw = run.getDrawProbability();
        double away = run.getAwayWinProbability();

        if (home >= draw && home >= away) {
            return "HOME";
        }
        if (away >= home && away >= draw) {
            return "AWAY";
        }
        return "DRAW";
    }

    private double brierScore1x2(PredictionRun run, String actualResultCode) {
        double oHome = "HOME".equals(actualResultCode) ? 1.0 : 0.0;
        double oDraw = "DRAW".equals(actualResultCode) ? 1.0 : 0.0;
        double oAway = "AWAY".equals(actualResultCode) ? 1.0 : 0.0;

        double sum =
                square(run.getHomeWinProbability() - oHome) +
                square(run.getDrawProbability() - oDraw) +
                square(run.getAwayWinProbability() - oAway);

        return sum / 3.0;
    }

    private double logLoss1x2(PredictionRun run, String actualResultCode) {
        double p;
        if ("HOME".equals(actualResultCode)) {
            p = run.getHomeWinProbability();
        } else if ("AWAY".equals(actualResultCode)) {
            p = run.getAwayWinProbability();
        } else {
            p = run.getDrawProbability();
        }

        p = clampProbability(p);
        return -Math.log(p);
    }

    private double square(double value) {
        return value * value;
    }

    private double clampProbability(double value) {
        return Math.max(1e-15, Math.min(1.0 - 1e-15, value));
    }

    private MarketProbabilities extractMarketProbabilities(Fixture fixture) {
        List<OddsSnapshot> odds = oddsSnapshotRepository.findByFixtureIdOrderByCapturedAtDesc(fixture.getId());

        Double homeOdd = null;
        Double drawOdd = null;
        Double awayOdd = null;

        for (OddsSnapshot snapshot : odds) {
            if (!is1x2Market(snapshot.getMarketName())) {
                continue;
            }

            String outcome = normaliseOutcome(snapshot.getOutcomeName(), fixture);
            if (outcome == null || snapshot.getOddValue() == null || snapshot.getOddValue() <= 0.0) {
                continue;
            }

            if ("HOME".equals(outcome) && homeOdd == null) {
                homeOdd = snapshot.getOddValue();
            } else if ("DRAW".equals(outcome) && drawOdd == null) {
                drawOdd = snapshot.getOddValue();
            } else if ("AWAY".equals(outcome) && awayOdd == null) {
                awayOdd = snapshot.getOddValue();
            }

            if (homeOdd != null && drawOdd != null && awayOdd != null) {
                break;
            }
        }

        if (homeOdd == null || drawOdd == null || awayOdd == null) {
            return null;
        }

        double homeImp = 1.0 / homeOdd;
        double drawImp = 1.0 / drawOdd;
        double awayImp = 1.0 / awayOdd;

        double total = homeImp + drawImp + awayImp;

        return new MarketProbabilities(
                homeImp / total,
                drawImp / total,
                awayImp / total
        );
    }

    private boolean is1x2Market(String marketName) {
        if (marketName == null) {
            return false;
        }
        String value = marketName.trim().toLowerCase();
        return value.equals("match winner") || value.equals("winner") || value.equals("1x2");
    }

    private String normaliseOutcome(String outcomeName, Fixture fixture) {
        if (outcomeName == null) {
            return null;
        }

        String value = outcomeName.trim().toLowerCase();
        String homeName = fixture.getHomeTeam() != null ? fixture.getHomeTeam().getTeamName().trim().toLowerCase() : "";
        String awayName = fixture.getAwayTeam() != null ? fixture.getAwayTeam().getTeamName().trim().toLowerCase() : "";

        if (value.equals("1") || value.equals("home") || value.equals(homeName)) {
            return "HOME";
        }
        if (value.equals("x") || value.equals("draw")) {
            return "DRAW";
        }
        if (value.equals("2") || value.equals("away") || value.equals(awayName)) {
            return "AWAY";
        }

        return null;
    }

    private record MarketProbabilities(double home, double draw, double away) {
    }
}