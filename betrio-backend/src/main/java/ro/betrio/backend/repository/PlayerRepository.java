package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.betrio.backend.domain.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByProviderNameAndExternalPlayerId(String providerName, Long externalPlayerId);
}