package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.TeamPlayerSquad;

public interface TeamPlayerSquadRepository extends JpaRepository<TeamPlayerSquad, Long> {
    void deleteBySeasonIdAndTeamId(Long seasonId, Long teamId);
    Optional<TeamPlayerSquad> findBySeasonIdAndTeamIdAndPlayerId(Long seasonId, Long teamId, Long playerId);
}