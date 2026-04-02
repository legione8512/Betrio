package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.FixtureLineup;

public interface FixtureLineupRepository extends JpaRepository<FixtureLineup, Long> {
    void deleteByFixtureId(Long fixtureId);
    Optional<FixtureLineup> findByFixtureIdAndTeamIdAndLineupTypeAndPlayerName(
            Long fixtureId, Long teamId, String lineupType, String playerName);
}