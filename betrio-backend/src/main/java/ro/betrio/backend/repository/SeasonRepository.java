package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.betrio.backend.domain.entity.Competition;
import ro.betrio.backend.domain.entity.Season;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByCompetitionAndExternalSeasonYear(Competition competition, Integer externalSeasonYear);
    
    java.util.Optional<Season> findByExternalSeasonYear(Integer externalSeasonYear);
}