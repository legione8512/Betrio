package ro.betrio.backend.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.PredictionRun;

public interface PredictionRunRepository extends JpaRepository<PredictionRun, Long> {
    List<PredictionRun> findByFixtureIdOrderByGeneratedAtDesc(Long fixtureId);
    Optional<PredictionRun> findTopByFixtureIdOrderByGeneratedAtDesc(Long fixtureId);
    @Query("""
    	    select prediction
    	    from PredictionRun prediction
    	    where prediction.fixture.id in :fixtureIds
    	      and not exists (
    	          select newerPrediction.id
    	          from PredictionRun newerPrediction
    	          where newerPrediction.fixture.id = prediction.fixture.id
    	            and (
    	                newerPrediction.generatedAt > prediction.generatedAt
    	                or (
    	                    newerPrediction.generatedAt = prediction.generatedAt
    	                    and newerPrediction.id > prediction.id
    	                )
    	            )
    	      )
    	    """)
    	List<PredictionRun> findLatestForFixtureIds(
    	        @Param("fixtureIds") Collection<Long> fixtureIds
    	);
    
    @Query("""
            select prediction
            from PredictionRun prediction
            join fetch prediction.fixture fixture
            join fetch fixture.homeTeam
            join fetch fixture.awayTeam
            where fixture.statusShort in ('FT', 'AET', 'PEN')
              and prediction.generatedAt < fixture.kickoffAt
              and not exists (
                  select evaluation.id
                  from PredictionEvaluation evaluation
                  where evaluation.predictionRun.id = prediction.id
              )
              and not exists (
                  select newerPrediction.id
                  from PredictionRun newerPrediction
                  where newerPrediction.fixture.id = fixture.id
                    and newerPrediction.generatedAt < fixture.kickoffAt
                    and (
                        newerPrediction.generatedAt > prediction.generatedAt
                        or (
                            newerPrediction.generatedAt = prediction.generatedAt
                            and newerPrediction.id > prediction.id
                        )
                    )
              )
            order by fixture.kickoffAt desc
            """)
    List<PredictionRun> findLatestUnevaluatedPreMatchRunsForFinishedFixtures(Pageable pageable);
}