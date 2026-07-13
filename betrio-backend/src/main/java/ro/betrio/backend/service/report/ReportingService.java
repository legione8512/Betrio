package ro.betrio.backend.service.report;

import java.util.List;
import ro.betrio.backend.api.dto.report.ValueBetReportDto;
import ro.betrio.backend.api.dto.report.ValueBetSelectionReportDto;
import java.util.ArrayList;
import ro.betrio.backend.api.dto.report.CalibrationBucketDto;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.api.dto.report.ModelVersionPerformanceDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.report.FixtureEvaluationReportDto;
import ro.betrio.backend.api.dto.report.ModelDashboardDto;
import ro.betrio.backend.api.dto.report.RecentEvaluationDto;
import ro.betrio.backend.api.dto.report.TeamPerformanceReportDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.PredictionEvaluation;
import ro.betrio.backend.repository.PredictionEvaluationRepository;

@Service
public class ReportingService {

    private final PredictionEvaluationRepository predictionEvaluationRepository;

    public ReportingService(
            PredictionEvaluationRepository predictionEvaluationRepository) {
        this.predictionEvaluationRepository = predictionEvaluationRepository;
    }

    @Transactional(readOnly = true)
    public ModelDashboardDto getDashboard() {
        return new ModelDashboardDto(
                predictionEvaluationRepository.countAllEvaluations(),
                safe(predictionEvaluationRepository.averageAccuracy1x2()),
                safe(predictionEvaluationRepository.averageAccuracyOver25()),
                safe(predictionEvaluationRepository.averageAccuracyBtts()),
                safe(predictionEvaluationRepository.averageExactScoreHitRate()),
                safe(predictionEvaluationRepository.averageBrierScore()),
                safe(predictionEvaluationRepository.averageLogLoss())
        );
    }

    @Transactional(readOnly = true)
    public List<RecentEvaluationDto> getRecentEvaluations(int limit) {
        return predictionEvaluationRepository.findRecentEvaluations(PageRequest.of(0, limit))
                .stream()
                .map(this::toRecentEvaluationDto)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ModelVersionPerformanceDto> getModelVersionPerformance() {
        return predictionEvaluationRepository.findModelVersionPerformance();
    }
    
    @Transactional(readOnly = true)
    public List<CalibrationBucketDto> getCalibrationBuckets() {
        List<PredictionEvaluation> evaluations =
                predictionEvaluationRepository.findAllForCalibration();

        List<CalibrationAccumulator> oneXtwoBuckets = newCalibrationBuckets();
        List<CalibrationAccumulator> over25Buckets = newCalibrationBuckets();
        List<CalibrationAccumulator> bttsBuckets = newCalibrationBuckets();

        for (PredictionEvaluation evaluation : evaluations) {
            PredictionRun run = evaluation.getPredictionRun();

            if (run == null) {
                continue;
            }

            addCalibrationPoint(
                    oneXtwoBuckets,
                    selectedOneXtwoProbability(evaluation),
                    evaluation.getHit1x2()
            );

            addCalibrationPoint(
                    over25Buckets,
                    Math.max(
                            safeProbability(run.getOver25Probability()),
                            safeProbability(run.getUnder25Probability())
                    ),
                    evaluation.getHitOver25()
            );

            addCalibrationPoint(
                    bttsBuckets,
                    Math.max(
                            safeProbability(run.getBttsYesProbability()),
                            safeProbability(run.getBttsNoProbability())
                    ),
                    evaluation.getHitBtts()
            );
        }

        List<CalibrationBucketDto> result = new ArrayList<>();

        appendCalibrationBuckets(result, "1X2", oneXtwoBuckets);
        appendCalibrationBuckets(result, "Over 2.5", over25Buckets);
        appendCalibrationBuckets(result, "BTTS", bttsBuckets);

        return result;
    }
    
    @Transactional(readOnly = true)
    public ValueBetReportDto getValueBetReport(double minEdge) {
        double safeMinEdge = Math.max(0.0, Math.min(0.50, minEdge));

        List<PredictionEvaluation> evaluations =
                predictionEvaluationRepository
                        .findLatestPreMatchEvaluationPerFixture();

        ValueBetAccumulator total = new ValueBetAccumulator("TOTAL");
        ValueBetAccumulator home = new ValueBetAccumulator("HOME");
        ValueBetAccumulator draw = new ValueBetAccumulator("DRAW");
        ValueBetAccumulator away = new ValueBetAccumulator("AWAY");

        long marketEvaluations = 0;

        for (PredictionEvaluation evaluation : evaluations) {
            BetCandidate candidate = bestValueCandidate(evaluation);

            if (candidate == null) {
                continue;
            }

            marketEvaluations++;

            if (candidate.edge() < safeMinEdge) {
                continue;
            }

            boolean won = candidate.selection().equals(evaluation.getActualResultCode());
            double profit = won ? candidate.odd() - 1.0 : -1.0;

            total.add(candidate.edge(), candidate.odd(), profit, won);

            if ("HOME".equals(candidate.selection())) {
                home.add(candidate.edge(), candidate.odd(), profit, won);
            } else if ("DRAW".equals(candidate.selection())) {
                draw.add(candidate.edge(), candidate.odd(), profit, won);
            } else if ("AWAY".equals(candidate.selection())) {
                away.add(candidate.edge(), candidate.odd(), profit, won);
            }
        }

        return new ValueBetReportDto(
                safeMinEdge,
                evaluations.size(),
                marketEvaluations,
                total.virtualBets,
                total.winningBets,
                total.losingBets,
                total.totalStake(),
                total.profit,
                total.roi(),
                total.winRate(),
                total.averageEdge(),
                total.averageOdd(),
                List.of(
                        home.toDto(),
                        draw.toDto(),
                        away.toDto()
                )
        );
    }

    @Transactional(readOnly = true)
    public FixtureEvaluationReportDto getFixtureReport(
            Long fixtureId) {

        PredictionEvaluation evaluation =
                predictionEvaluationRepository
                        .findDetailedByFixtureId(
                                fixtureId,
                                PageRequest.of(0, 1)
                        )
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No pre-match evaluation found "
                                                + "for fixture id: "
                                                + fixtureId
                                )
                        );
    	
        Fixture fixture = evaluation.getPredictionRun().getFixture();

        return new FixtureEvaluationReportDto(
                fixture.getId(),
                fixture.getHomeTeam().getTeamName(),
                fixture.getAwayTeam().getTeamName(),
                fixture.getKickoffAt(),
                evaluation.getPredictedResultCode(),
                evaluation.getActualResultCode(),
                evaluation.getActualHomeGoals(),
                evaluation.getActualAwayGoals(),
                evaluation.getHit1x2(),
                evaluation.getHitOver25(),
                evaluation.getHitBtts(),
                evaluation.getTopExactScoreHit(),
                evaluation.getBrierScore1x2(),
                evaluation.getLogLoss1x2()
        );
    }

    @Transactional(readOnly = true)
    public TeamPerformanceReportDto getTeamReport(Long teamId) {
        List<PredictionEvaluation> evaluations = predictionEvaluationRepository.findByTeamId(teamId);

        if (evaluations.isEmpty()) {
        	throw new ResponseStatusException(
        	        HttpStatus.NOT_FOUND,
        	        "No evaluated matches found for team id: " + teamId
        	);
        }

        String teamName = extractTeamName(teamId, evaluations.getFirst().getPredictionRun().getFixture());

        long total = evaluations.size();

        double acc1x2 = evaluations.stream().mapToDouble(e -> boolToDouble(e.getHit1x2())).average().orElse(0.0);
        double accOver25 = evaluations.stream().mapToDouble(e -> boolToDouble(e.getHitOver25())).average().orElse(0.0);
        double accBtts = evaluations.stream().mapToDouble(e -> boolToDouble(e.getHitBtts())).average().orElse(0.0);
        double avgBrier = evaluations.stream().mapToDouble(PredictionEvaluation::getBrierScore1x2).average().orElse(0.0);
        double avgLogLoss = evaluations.stream().mapToDouble(PredictionEvaluation::getLogLoss1x2).average().orElse(0.0);

        return new TeamPerformanceReportDto(
                teamId,
                teamName,
                total,
                acc1x2,
                accOver25,
                accBtts,
                avgBrier,
                avgLogLoss
        );
    }

    private RecentEvaluationDto toRecentEvaluationDto(PredictionEvaluation evaluation) {
        Fixture fixture = evaluation.getPredictionRun().getFixture();

        return new RecentEvaluationDto(
                evaluation.getId(),
                evaluation.getPredictionRun().getId(),
                fixture.getId(),
                fixture.getHomeTeam().getTeamName(),
                fixture.getAwayTeam().getTeamName(),
                fixture.getKickoffAt(),
                evaluation.getPredictedResultCode(),
                evaluation.getActualResultCode(),
                evaluation.getActualHomeGoals(),
                evaluation.getActualAwayGoals(),
                evaluation.getHit1x2(),
                evaluation.getHitOver25(),
                evaluation.getHitBtts(),
                evaluation.getTopExactScoreHit(),
                evaluation.getBrierScore1x2(),
                evaluation.getLogLoss1x2()
        );
    }

    private String extractTeamName(Long teamId, Fixture fixture) {
        if (fixture.getHomeTeam() != null && teamId.equals(fixture.getHomeTeam().getId())) {
            return fixture.getHomeTeam().getTeamName();
        }
        if (fixture.getAwayTeam() != null && teamId.equals(fixture.getAwayTeam().getId())) {
            return fixture.getAwayTeam().getTeamName();
        }
        return "Unknown";
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double boolToDouble(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1.0 : 0.0;
    }
    private List<CalibrationAccumulator> newCalibrationBuckets() {
        List<CalibrationAccumulator> buckets = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            buckets.add(new CalibrationAccumulator(i / 10.0, (i + 1) / 10.0));
        }

        return buckets;
    }

    private void addCalibrationPoint(
            List<CalibrationAccumulator> buckets,
            double predictedProbability,
            Boolean hit) {

        if (Double.isNaN(predictedProbability)
                || predictedProbability <= 0.0
                || hit == null) {
            return;
        }

        int bucketIndex =
                Math.min(9, Math.max(0, (int) Math.floor(predictedProbability * 10.0)));

        buckets.get(bucketIndex).add(predictedProbability, boolToDouble(hit));
    }

    private void appendCalibrationBuckets(
            List<CalibrationBucketDto> result,
            String market,
            List<CalibrationAccumulator> buckets) {

        buckets.stream()
                .filter(CalibrationAccumulator::hasEvaluations)
                .map(bucket -> bucket.toDto(market))
                .forEach(result::add);
    }

    private double selectedOneXtwoProbability(PredictionEvaluation evaluation) {
        PredictionRun run = evaluation.getPredictionRun();
        String resultCode = evaluation.getPredictedResultCode();

        if (run == null || resultCode == null) {
            return Double.NaN;
        }

        if ("HOME".equals(resultCode)) {
            return safeProbability(run.getHomeWinProbability());
        }

        if ("DRAW".equals(resultCode)) {
            return safeProbability(run.getDrawProbability());
        }

        if ("AWAY".equals(resultCode)) {
            return safeProbability(run.getAwayWinProbability());
        }

        return Double.NaN;
    }

    private double safeProbability(Double value) {
        return Math.max(0.0, Math.min(1.0, safe(value)));
    }

    private static class CalibrationAccumulator {
        private final double minProbability;
        private final double maxProbability;
        private long evaluations;
        private double predictedProbabilitySum;
        private double hitSum;

        private CalibrationAccumulator(double minProbability, double maxProbability) {
            this.minProbability = minProbability;
            this.maxProbability = maxProbability;
        }

        private void add(double predictedProbability, double hit) {
            evaluations++;
            predictedProbabilitySum += predictedProbability;
            hitSum += hit;
        }

        private boolean hasEvaluations() {
            return evaluations > 0;
        }

        private CalibrationBucketDto toDto(String market) {
            double averagePredictedProbability = predictedProbabilitySum / evaluations;
            double actualHitRate = hitSum / evaluations;

            return new CalibrationBucketDto(
                    market,
                    bucketLabel(),
                    minProbability,
                    maxProbability,
                    evaluations,
                    averagePredictedProbability,
                    actualHitRate,
                    actualHitRate - averagePredictedProbability
            );
        }

        private String bucketLabel() {
            return Math.round(minProbability * 100)
                    + "-"
                    + Math.round(maxProbability * 100)
                    + "%";
        }
    }
    private BetCandidate bestValueCandidate(PredictionEvaluation evaluation) {
        List<BetCandidate> candidates = new ArrayList<>();

        addCandidate(candidates, "HOME", evaluation.getModelEdgeHome(), evaluation.getMarketHomeOdd());
        addCandidate(candidates, "DRAW", evaluation.getModelEdgeDraw(), evaluation.getMarketDrawOdd());
        addCandidate(candidates, "AWAY", evaluation.getModelEdgeAway(), evaluation.getMarketAwayOdd());

        return candidates.stream()
                .max((left, right) -> Double.compare(left.edge(), right.edge()))
                .orElse(null);
    }

    private void addCandidate(
            List<BetCandidate> candidates,
            String selection,
            Double edge,
            Double odd) {

        if (edge == null || odd == null || odd <= 1.0) {
            return;
        }

        candidates.add(new BetCandidate(selection, edge, odd));
    }

    private record BetCandidate(
            String selection,
            double edge,
            double odd
    ) {
    }

    private static class ValueBetAccumulator {
        private final String selection;
        private long virtualBets;
        private long winningBets;
        private long losingBets;
        private double profit;
        private double edgeSum;
        private double oddSum;

        private ValueBetAccumulator(String selection) {
            this.selection = selection;
        }

        private void add(double edge, double odd, double betProfit, boolean won) {
            virtualBets++;
            profit += betProfit;
            edgeSum += edge;
            oddSum += odd;

            if (won) {
                winningBets++;
            } else {
                losingBets++;
            }
        }

        private double totalStake() {
            return virtualBets;
        }

        private double roi() {
            return virtualBets == 0 ? 0.0 : profit / totalStake();
        }

        private double winRate() {
            return virtualBets == 0 ? 0.0 : (double) winningBets / virtualBets;
        }

        private double averageEdge() {
            return virtualBets == 0 ? 0.0 : edgeSum / virtualBets;
        }

        private double averageOdd() {
            return virtualBets == 0 ? 0.0 : oddSum / virtualBets;
        }

        private ValueBetSelectionReportDto toDto() {
            return new ValueBetSelectionReportDto(
                    selection,
                    virtualBets,
                    winningBets,
                    losingBets,
                    totalStake(),
                    profit,
                    roi(),
                    winRate(),
                    averageEdge(),
                    averageOdd()
            );
        }
    }
}