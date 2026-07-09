package ro.betrio.backend.service.prediction;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.BatchEvaluationResultDto;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.PredictionRunRepository;

@Service
public class PredictionBatchEvaluationService {

    private final PredictionRunRepository predictionRunRepository;
    private final PredictionEvaluationService predictionEvaluationService;

    public PredictionBatchEvaluationService(
            PredictionRunRepository predictionRunRepository,
            PredictionEvaluationService predictionEvaluationService) {
        this.predictionRunRepository = predictionRunRepository;
        this.predictionEvaluationService = predictionEvaluationService;
    }

    @Transactional(readOnly = true)
    public List<PredictionRun> findCandidates(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));

        return predictionRunRepository
                .findLatestUnevaluatedPreMatchRunsForFinishedFixtures(
                        PageRequest.of(0, safeLimit)
                );
    }

    public BatchEvaluationResultDto evaluateFinishedPreMatchPredictions(int limit) {
        List<PredictionRun> candidates = findCandidates(limit);

        int evaluated = 0;
        int failed = 0;
        List<String> messages = new ArrayList<>();

        for (PredictionRun run : candidates) {
            try {
                Long evaluationId =
                        predictionEvaluationService.evaluatePredictionRun(run.getId());

                evaluated++;

                if (messages.size() < 20) {
                    messages.add(
                            "OK fixtureId="
                                    + run.getFixture().getId()
                                    + ", predictionRunId="
                                    + run.getId()
                                    + ", evaluationId="
                                    + evaluationId
                    );
                }
            } catch (Exception ex) {
                failed++;

                if (messages.size() < 20) {
                    messages.add(
                            "FAILED predictionRunId="
                                    + run.getId()
                                    + ": "
                                    + ex.getMessage()
                    );
                }
            }
        }

        return new BatchEvaluationResultDto(
                candidates.size(),
                evaluated,
                failed,
                messages
        );
    }
}