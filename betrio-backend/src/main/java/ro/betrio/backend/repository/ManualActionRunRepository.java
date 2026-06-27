package ro.betrio.backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ro.betrio.backend.domain.entity.ManualActionRun;

public interface ManualActionRunRepository extends JpaRepository<ManualActionRun, Long> {

    List<ManualActionRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<ManualActionRun> findByFixtureIdOrderByStartedAtDesc(Long fixtureId, Pageable pageable);
}