package ro.betrio.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.PredictionRun;

public interface PredictionRunRepository extends JpaRepository<PredictionRun, Long> {
    List<PredictionRun> findByFixtureIdOrderByGeneratedAtDesc(Long fixtureId);
    Optional<PredictionRun> findTopByFixtureIdOrderByGeneratedAtDesc(Long fixtureId);
}