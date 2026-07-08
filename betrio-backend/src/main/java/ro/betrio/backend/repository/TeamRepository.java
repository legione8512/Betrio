package ro.betrio.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.betrio.backend.domain.entity.Season;
import ro.betrio.backend.domain.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findBySeasonAndProviderNameAndExternalTeamId(Season season, String providerName, Long externalTeamId);

    List<Team> findBySeason(Season season);

    @Query("""
            select distinct t
            from Team t
            order by t.teamName asc
            """)
    List<Team> findAllOrderedByTeamName();

    @Query("""
            select t
            from Team t
            where t.season.competition.id = :competitionId
            order by t.teamName asc, t.season.externalSeasonYear desc
            """)
    List<Team> findAllByCompetitionOrderedByTeamName(@Param("competitionId") Long competitionId);
}