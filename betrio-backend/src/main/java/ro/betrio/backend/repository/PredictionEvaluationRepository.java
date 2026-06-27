package ro.betrio.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.betrio.backend.domain.entity.PredictionEvaluation;

public interface PredictionEvaluationRepository extends JpaRepository<PredictionEvaluation, Long> {

    Optional<PredictionEvaluation> findByPredictionRunId(Long predictionRunId);

    @Query("""
        select count(pe)
        from PredictionEvaluation pe
        """)
    long countAllEvaluations();

    @Query("""
        select avg(case when pe.hit1x2 = true then 1.0 else 0.0 end)
        from PredictionEvaluation pe
        """)
    Double averageAccuracy1x2();

    @Query("""
        select avg(case when pe.hitOver25 = true then 1.0 else 0.0 end)
        from PredictionEvaluation pe
        """)
    Double averageAccuracyOver25();

    @Query("""
        select avg(case when pe.hitBtts = true then 1.0 else 0.0 end)
        from PredictionEvaluation pe
        """)
    Double averageAccuracyBtts();

    @Query("""
        select avg(case when pe.topExactScoreHit = true then 1.0 else 0.0 end)
        from PredictionEvaluation pe
        """)
    Double averageExactScoreHitRate();

    @Query("""
        select avg(pe.brierScore1x2)
        from PredictionEvaluation pe
        """)
    Double averageBrierScore();

    @Query("""
        select avg(pe.logLoss1x2)
        from PredictionEvaluation pe
        """)
    Double averageLogLoss();

    @Query("""
        select pe
        from PredictionEvaluation pe
        join fetch pe.predictionRun pr
        join fetch pr.fixture f
        join fetch f.homeTeam
        join fetch f.awayTeam
        order by pe.evaluatedAt desc
        """)
    List<PredictionEvaluation> findRecentEvaluations(Pageable pageable);

    @Query("""
        select pe
        from PredictionEvaluation pe
        join fetch pe.predictionRun pr
        join fetch pr.fixture f
        join fetch f.homeTeam
        join fetch f.awayTeam
        where f.id = :fixtureId
        """)
    Optional<PredictionEvaluation> findDetailedByFixtureId(Long fixtureId);

    @Query("""
        select pe
        from PredictionEvaluation pe
        join fetch pe.predictionRun pr
        join fetch pr.fixture f
        join fetch f.homeTeam
        join fetch f.awayTeam
        where f.homeTeam.id = :teamId or f.awayTeam.id = :teamId
        order by pe.evaluatedAt desc
        """)
    List<PredictionEvaluation> findByTeamId(Long teamId);
}