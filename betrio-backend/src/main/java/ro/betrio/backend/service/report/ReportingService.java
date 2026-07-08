package ro.betrio.backend.service.report;

import java.util.List;

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
    public FixtureEvaluationReportDto getFixtureReport(Long fixtureId) {
        PredictionEvaluation evaluation = predictionEvaluationRepository.findDetailedByFixtureId(fixtureId)
                .orElseThrow(() -> new IllegalStateException("No evaluation found for fixture id: " + fixtureId));

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
            throw new IllegalStateException("No evaluated matches found for team id: " + teamId);
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
}