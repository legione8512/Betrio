package ro.betrio.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.OddsSnapshot;

public interface OddsSnapshotRepository extends JpaRepository<OddsSnapshot, Long> {
    void deleteByFixtureId(Long fixtureId);
    List<OddsSnapshot> findByFixtureIdOrderByCapturedAtDesc(Long fixtureId);
}