package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.betrio.backend.domain.entity.FixtureTeam;

public interface FixtureTeamRepository extends JpaRepository<FixtureTeam, Long> {
    Optional<FixtureTeam> findByFixtureIdAndSide(Long fixtureId, String side);
}