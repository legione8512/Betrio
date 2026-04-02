package ro.betrio.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.PredictionExactScore;

public interface PredictionExactScoreRepository extends JpaRepository<PredictionExactScore, Long> {
    List<PredictionExactScore> findByPredictionRunIdOrderByProbabilityDesc(Long predictionRunId);
}