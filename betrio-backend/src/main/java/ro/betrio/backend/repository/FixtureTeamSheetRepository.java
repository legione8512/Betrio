package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.FixtureTeamSheet;

public interface FixtureTeamSheetRepository extends JpaRepository<FixtureTeamSheet, Long> {
    void deleteByFixtureId(Long fixtureId);
    Optional<FixtureTeamSheet> findByFixtureIdAndTeamId(Long fixtureId, Long teamId);
}