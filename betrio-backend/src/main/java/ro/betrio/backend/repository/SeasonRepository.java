package ro.betrio.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.betrio.backend.domain.entity.Competition;
import ro.betrio.backend.domain.entity.Season;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    Optional<Season> findByCompetitionAndExternalSeasonYear(
            Competition competition,
            Integer externalSeasonYear
    );

    Optional<Season> findByExternalSeasonYear(Integer externalSeasonYear);

    @Query("""
            select s
            from Season s
            join fetch s.competition
            where s.currentFlag = true
            order by s.externalSeasonYear desc
            """)
    List<Season> findCurrentSeasonsWithCompetition();

    @Query("""
            select s
            from Season s
            join fetch s.competition
            where s.id = :seasonId
            """)
    Optional<Season> findDetailedById(@Param("seasonId") Long seasonId);

    @Query("""
            select distinct s
            from Season s
            join fetch s.competition
            join Fixture f on f.season.id = s.id
            where f.statusShort in ('FT', 'AET', 'PEN')
            order by s.externalSeasonYear desc, s.id desc
            """)
    List<Season> findSeasonsWithCompletedFixtures();
    @Query("""
            select s
            from Season s
            join fetch s.competition
            where s.competition.id = :competitionId
              and s.currentFlag = true
            order by s.externalSeasonYear desc
            """)
    List<Season> findCurrentSeasonsByCompetition(@Param("competitionId") Long competitionId);
    @Query("""
            select distinct s
            from Season s
            join fetch s.competition
            join Fixture f on f.season.id = s.id
            where s.competition.id = :competitionId
              and f.statusShort in ('FT', 'AET', 'PEN')
            order by s.externalSeasonYear desc, s.id desc
            """)
    List<Season> findSeasonsWithCompletedFixturesByCompetition(@Param("competitionId") Long competitionId);
}