package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.Season;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {
    Optional<Fixture> findBySeasonAndProviderNameAndExternalFixtureId(Season season, String providerName, Long externalFixtureId);
    
    java.util.List<Fixture> findBySeasonAndKickoffAtBetween(
            Season season,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to
    );
    
    @Query("""
    		select f from Fixture f
    		where (f.homeTeam.id = :teamId or f.awayTeam.id = :teamId)
    		and f.kickoffAt < :before
    		and f.statusShort in ('FT', 'AET', 'PEN')
    		order by f.kickoffAt desc
    		""")
    		List<Fixture> findRecentCompletedFixturesForTeam(
    		        @Param("teamId") Long teamId,
    		        @Param("before") OffsetDateTime before,
    		        Pageable pageable
    		);

    		@Query("""
    		select f from Fixture f
    		where (
    		    (f.homeTeam.id = :homeTeamId and f.awayTeam.id = :awayTeamId)
    		    or
    		    (f.homeTeam.id = :awayTeamId and f.awayTeam.id = :homeTeamId)
    		)
    		and f.kickoffAt < :before
    		and f.statusShort in ('FT', 'AET', 'PEN')
    		order by f.kickoffAt desc
    		""")
    		List<Fixture> findRecentHeadToHead(
    		        @Param("homeTeamId") Long homeTeamId,
    		        @Param("awayTeamId") Long awayTeamId,
    		        @Param("before") OffsetDateTime before,
    		        Pageable pageable
    		);

    		@Query("""
    		select f from Fixture f
    		where f.season = :season
    		and f.statusShort in ('FT', 'AET', 'PEN')
    		order by f.kickoffAt desc
    		""")
    		List<Fixture> findCompletedFixturesForSeason(
    		        @Param("season") Season season,
    		        Pageable pageable
    		);
}