package ro.betrio.backend.service.app;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.RecentEvaluationDto;
import ro.betrio.backend.domain.entity.PredictionEvaluation;
import ro.betrio.backend.repository.PredictionEvaluationRepository;

@Service
public class AppEvaluationService {

    private final PredictionEvaluationRepository predictionEvaluationRepository;

    public AppEvaluationService(PredictionEvaluationRepository predictionEvaluationRepository) {
        this.predictionEvaluationRepository = predictionEvaluationRepository;
    }

    @Transactional(readOnly = true)
    public List<RecentEvaluationDto> getRecentEvaluations(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        return predictionEvaluationRepository.findRecentWithFixture(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private RecentEvaluationDto toDto(PredictionEvaluation pe) {
        return new RecentEvaluationDto(
                pe.getId(),
                pe.getPredictionRun().getId(),
                pe.getPredictionRun().getFixture().getId(),
                pe.getPredictionRun().getFixture().getHomeTeam().getTeamName(),
                pe.getPredictionRun().getFixture().getAwayTeam().getTeamName(),
                pe.getPredictionRun().getFixture().getKickoffAt(),
                pe.getPredictedResultCode(),
                pe.getActualResultCode(),
                pe.getActualHomeGoals(),
                pe.getActualAwayGoals(),
                pe.getHit1x2(),
                pe.getHitOver25(),
                pe.getHitBtts(),
                pe.getTopExactScoreHit(),
                pe.getBrierScore1x2(),
                pe.getLogLoss1x2()
        );
    }
}