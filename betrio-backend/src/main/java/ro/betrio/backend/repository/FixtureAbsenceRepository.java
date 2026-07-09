package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.betrio.backend.domain.entity.FixtureAbsence;

public interface FixtureAbsenceRepository extends JpaRepository<FixtureAbsence, Long> {

    void deleteByFixtureId(Long fixtureId);

    Optional<FixtureAbsence> findByFixtureIdAndTeamIdAndPlayerNameAndAbsenceTypeAndReasonText(
            Long fixtureId,
            Long teamId,
            String playerName,
            String absenceType,
            String reasonText
    );

    long countByFixtureIdAndTeamId(Long fixtureId, Long teamId);

    @Query("""
            select count(a)
            from FixtureAbsence a
            where a.fixture.id = :fixtureId
              and a.team.id = :teamId
              and lower(coalesce(a.absenceType, '')) = lower(:absenceType)
            """)
    long countByFixtureIdAndTeamIdAndAbsenceType(
            @Param("fixtureId") Long fixtureId,
            @Param("teamId") Long teamId,
            @Param("absenceType") String absenceType
    );
}