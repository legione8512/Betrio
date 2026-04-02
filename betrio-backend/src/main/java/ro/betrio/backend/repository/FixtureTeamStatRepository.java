package ro.betrio.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.betrio.backend.domain.entity.FixtureTeamStat;

public interface FixtureTeamStatRepository extends JpaRepository<FixtureTeamStat, Long> {

    void deleteByFixtureId(Long fixtureId);

    List<FixtureTeamStat> findByFixtureId(Long fixtureId);

    List<FixtureTeamStat> findByFixtureIdAndTeamId(Long fixtureId, Long teamId);

    Optional<FixtureTeamStat> findByFixtureIdAndTeamIdAndStatName(Long fixtureId, Long teamId, String statName);
}