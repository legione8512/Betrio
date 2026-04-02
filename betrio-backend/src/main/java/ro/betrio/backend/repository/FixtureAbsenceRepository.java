package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.FixtureAbsence;

public interface FixtureAbsenceRepository extends JpaRepository<FixtureAbsence, Long> {
    void deleteByFixtureId(Long fixtureId);
    Optional<FixtureAbsence> findByFixtureIdAndTeamIdAndPlayerNameAndAbsenceTypeAndReasonText(
            Long fixtureId, Long teamId, String playerName, String absenceType, String reasonText);
    
    long countByFixtureIdAndTeamId(Long fixtureId, Long teamId);
}