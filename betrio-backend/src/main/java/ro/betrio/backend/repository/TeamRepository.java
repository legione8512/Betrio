package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.betrio.backend.domain.entity.Season;
import ro.betrio.backend.domain.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findBySeasonAndProviderNameAndExternalTeamId(Season season, String providerName, Long externalTeamId);
    
    java.util.List<Team> findBySeason(Season season);
}