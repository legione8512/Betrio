package ro.betrio.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.betrio.backend.domain.entity.Competition;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    Optional<Competition> findByProviderNameAndExternalLeagueId(String providerName, Long externalLeagueId);
    @Query("""
            select c
            from Competition c
            order by c.countryName asc, c.competitionName asc
            """)
    List<Competition> findAllOrdered();
    
}