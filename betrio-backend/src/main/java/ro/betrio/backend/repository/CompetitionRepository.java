package ro.betrio.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.betrio.backend.domain.entity.Competition;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    Optional<Competition> findByProviderNameAndExternalLeagueId(String providerName, Long externalLeagueId);
}