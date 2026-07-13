package ro.betrio.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.Season;

public interface FixtureRepository extends JpaRepository<Fixture, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Fixture>{

    Optional<Fixture> findBySeasonAndProviderNameAndExternalFixtureId(
            Season season,
            String providerName,
            Long externalFixtureId
    );

    List<Fixture> findBySeasonAndKickoffAtBetween(
            Season season,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where (
                    (ht.providerName = :providerName and ht.externalTeamId = :externalTeamId)
                    or
                    (at.providerName = :providerName and at.externalTeamId = :externalTeamId)
                  )
              and f.kickoffAt < :before
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by f.kickoffAt desc
            """)
    List<Fixture> findRecentCompletedFixturesForTeam(
            @Param("providerName") String providerName,
            @Param("externalTeamId") Long externalTeamId,
            @Param("before") OffsetDateTime before,
            Pageable pageable
    );
    
    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where ht.providerName = :providerName
              and ht.externalTeamId = :externalTeamId
              and f.kickoffAt < :before
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by f.kickoffAt desc
            """)
    List<Fixture> findRecentCompletedHomeFixturesForTeam(
            @Param("providerName") String providerName,
            @Param("externalTeamId") Long externalTeamId,
            @Param("before") OffsetDateTime before,
            Pageable pageable
    );

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where at.providerName = :providerName
              and at.externalTeamId = :externalTeamId
              and f.kickoffAt < :before
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by f.kickoffAt desc
            """)
    List<Fixture> findRecentCompletedAwayFixturesForTeam(
            @Param("providerName") String providerName,
            @Param("externalTeamId") Long externalTeamId,
            @Param("before") OffsetDateTime before,
            Pageable pageable
    );

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where (
                    (
                        ht.providerName = :providerName
                        and ht.externalTeamId = :homeExternalTeamId
                        and at.providerName = :providerName
                        and at.externalTeamId = :awayExternalTeamId
                    )
                    or
                    (
                        ht.providerName = :providerName
                        and ht.externalTeamId = :awayExternalTeamId
                        and at.providerName = :providerName
                        and at.externalTeamId = :homeExternalTeamId
                    )
                  )
              and f.kickoffAt < :before
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by f.kickoffAt desc
            """)
    List<Fixture> findRecentHeadToHead(
            @Param("providerName") String providerName,
            @Param("homeExternalTeamId") Long homeExternalTeamId,
            @Param("awayExternalTeamId") Long awayExternalTeamId,
            @Param("before") OffsetDateTime before,
            Pageable pageable
    );

    @Query("""
            select f
            from Fixture f
            where f.season = :season
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by f.kickoffAt desc
            """)
    List<Fixture> findCompletedFixturesForSeason(
            @Param("season") Season season,
            Pageable pageable
    );
    @Query("""
            select f
            from Fixture f
            where f.season = :season
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by f.kickoffAt asc
            """)
    List<Fixture> findAllCompletedFixturesForSeason(
            @Param("season") ro.betrio.backend.domain.entity.Season season
    );

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            where f.kickoffAt >= CURRENT_TIMESTAMP
            order by f.kickoffAt asc
            """)
    List<Fixture> findUpcomingWithTeams(Pageable pageable);

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            order by f.kickoffAt desc
            """)
    List<Fixture> findRecentWithTeams(Pageable pageable);

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            where f.id = :fixtureId
            """)
    Optional<Fixture> findDetailedById(@Param("fixtureId") Long fixtureId);

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where (:query is null or :query = '' or
                   lower(ht.teamName) like lower(concat('%', :query, '%')) or
                   lower(at.teamName) like lower(concat('%', :query, '%')) or
                   lower(coalesce(f.leagueRound, '')) like lower(concat('%', :query, '%')))
              and (:status is null or :status = '' or f.statusShort = :status)
            order by f.kickoffAt desc
            """)
    List<Fixture> searchWithTeamsNoDate(
            @Param("query") String query,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where (:query is null or :query = '' or
                   lower(ht.teamName) like lower(concat('%', :query, '%')) or
                   lower(at.teamName) like lower(concat('%', :query, '%')) or
                   lower(coalesce(f.leagueRound, '')) like lower(concat('%', :query, '%')))
              and (:status is null or :status = '' or f.statusShort = :status)
              and f.kickoffAt >= :fromDate
            order by f.kickoffAt desc
            """)
    List<Fixture> searchWithTeamsFromDate(
            @Param("query") String query,
            @Param("status") String status,
            @Param("fromDate") OffsetDateTime fromDate,
            Pageable pageable
    );

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where (:query is null or :query = '' or
                   lower(ht.teamName) like lower(concat('%', :query, '%')) or
                   lower(at.teamName) like lower(concat('%', :query, '%')) or
                   lower(coalesce(f.leagueRound, '')) like lower(concat('%', :query, '%')))
              and (:status is null or :status = '' or f.statusShort = :status)
              and f.kickoffAt <= :toDate
            order by f.kickoffAt desc
            """)
    List<Fixture> searchWithTeamsToDate(
            @Param("query") String query,
            @Param("status") String status,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable
    );

    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam ht
            join fetch f.awayTeam at
            where (:query is null or :query = '' or
                   lower(ht.teamName) like lower(concat('%', :query, '%')) or
                   lower(at.teamName) like lower(concat('%', :query, '%')) or
                   lower(coalesce(f.leagueRound, '')) like lower(concat('%', :query, '%')))
              and (:status is null or :status = '' or f.statusShort = :status)
              and f.kickoffAt >= :fromDate
              and f.kickoffAt <= :toDate
            order by f.kickoffAt desc
            """)
    List<Fixture> searchWithTeamsBetweenDates(
            @Param("query") String query,
            @Param("status") String status,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable
    );
    @Query("""
            select distinct f.statusShort
            from Fixture f
            where f.statusShort is not null
            order by f.statusShort asc
            """)
    List<String> findDistinctStatuses();

    @Query("""
            select distinct f.leagueRound
            from Fixture f
            where f.leagueRound is not null and f.leagueRound <> ''
            order by f.leagueRound asc
            """)
    List<String> findDistinctRounds();

    @Query("""
            select count(f)
            from Fixture f
            where f.kickoffAt >= CURRENT_TIMESTAMP
            """)
    long countUpcomingFixtures();

    @Query("""
            select count(f)
            from Fixture f
            where f.statusShort in ('FT', 'AET', 'PEN')
            """)
    long countFinishedFixtures();
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "homeTeam", "awayTeam" })
    org.springframework.data.domain.Page<Fixture> findAll(
            org.springframework.data.jpa.domain.Specification<Fixture> spec,
            org.springframework.data.domain.Pageable pageable
    );
    @Query("""
            select f
            from Fixture f
            where (f.homeTeam.id = :teamId or f.awayTeam.id = :teamId)
              and f.kickoffAt >= :after
            order by f.kickoffAt asc
            """)
    List<Fixture> findUpcomingFixturesForTeam(
            @Param("teamId") Long teamId,
            @Param("after") OffsetDateTime after,
            Pageable pageable
    );
    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            where f.season.competition.id = :competitionId
              and f.kickoffAt >= CURRENT_TIMESTAMP
            order by f.kickoffAt asc
            """)
    List<Fixture> findUpcomingWithTeamsByCompetition(
            @Param("competitionId") Long competitionId,
            Pageable pageable
    );
    
    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            where f.season.competition.id = :competitionId
              and f.kickoffAt >= :from
              and f.kickoffAt < :untilExclusive
            order by f.kickoffAt asc
            """)
    List<Fixture> findUpcomingWithTeamsByCompetitionInWindow(
            @Param("competitionId") Long competitionId,
            @Param("from") OffsetDateTime from,
            @Param("untilExclusive") OffsetDateTime untilExclusive,
            Pageable pageable
    );
    
    @Query("""
            select distinct f.leagueRound
            from Fixture f
            where f.season.competition.id = :competitionId
              and f.leagueRound is not null
              and f.leagueRound <> ''
            order by f.leagueRound asc
            """)
    List<String> findDistinctRoundsByCompetition(
            @Param("competitionId") Long competitionId
    );
    @Query("""
            select f
            from Fixture f
            join fetch f.homeTeam
            join fetch f.awayTeam
            where f.season.competition.id = :competitionId
              and f.kickoffAt < :before
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by f.kickoffAt desc
            """)
    List<Fixture> findCompletedFixturesForCompetitionBefore(
            @Param("competitionId") Long competitionId,
            @Param("before") OffsetDateTime before,
            Pageable pageable
    );
}