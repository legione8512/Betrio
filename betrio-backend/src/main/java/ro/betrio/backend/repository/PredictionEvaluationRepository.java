package ro.betrio.backend.repository;

import java.util.List;
import ro.betrio.backend.api.dto.report.ModelVersionPerformanceDto;
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
              and pr.generatedAt < f.kickoffAt
            order by pr.generatedAt desc, pe.id desc
            """)
    List<PredictionEvaluation> findDetailedByFixtureId(
            Long fixtureId,
            Pageable pageable
    );

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
    @Query("""
            select pe
            from PredictionEvaluation pe
            join fetch pe.predictionRun pr
            join fetch pr.fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            order by pe.evaluatedAt desc
            """)
    List<PredictionEvaluation> findRecentWithFixture(Pageable pageable);
    
    @Query("""
            select new ro.betrio.backend.api.dto.report.ModelVersionPerformanceDto(
                coalesce(pr.modelVersion, 'unknown'),
                count(e),
                coalesce(avg(case when e.hit1x2 = true then 1.0 else 0.0 end), 0.0),
                coalesce(avg(case when e.hitOver25 = true then 1.0 else 0.0 end), 0.0),
                coalesce(avg(case when e.hitBtts = true then 1.0 else 0.0 end), 0.0),
                coalesce(avg(case when e.topExactScoreHit = true then 1.0 else 0.0 end), 0.0),
                coalesce(avg(e.brierScore1x2), 0.0),
                coalesce(avg(e.logLoss1x2), 0.0)
            )
            from PredictionEvaluation e
            join e.predictionRun pr
            group by coalesce(pr.modelVersion, 'unknown')
            order by count(e) desc
            """)
    List<ModelVersionPerformanceDto> findModelVersionPerformance();
    
    @Query("""
            select pe
            from PredictionEvaluation pe
            join fetch pe.predictionRun pr
            order by pe.evaluatedAt desc
            """)
    List<PredictionEvaluation> findAllForCalibration();
    @Query("""
            select pe
            from PredictionEvaluation pe
            join fetch pe.predictionRun pr
            join fetch pr.fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            where pr.generatedAt < f.kickoffAt
              and not exists (
                  select newerEvaluation.id
                  from PredictionEvaluation newerEvaluation
                  join newerEvaluation.predictionRun newerPrediction
                  where newerPrediction.fixture.id = f.id
                    and newerPrediction.generatedAt < f.kickoffAt
                    and (
                        newerPrediction.generatedAt > pr.generatedAt
                        or (
                            newerPrediction.generatedAt = pr.generatedAt
                            and newerPrediction.id > pr.id
                        )
                    )
              )
            order by f.kickoffAt desc
            """)
    List<PredictionEvaluation>
            findLatestPreMatchEvaluationPerFixture();
}