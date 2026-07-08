package ro.betrio.backend.repository;

import java.util.List;
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
}