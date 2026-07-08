package ro.betrio.backend.service.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.CompetitionOptionDto;
import ro.betrio.backend.repository.CompetitionRepository;

@Service
public class AppCompetitionService {

    private final CompetitionRepository competitionRepository;

    public AppCompetitionService(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
    }

    @Transactional(readOnly = true)
    public List<CompetitionOptionDto> getCompetitions() {
        return competitionRepository.findAllOrdered()
                .stream()
                .map(c -> new CompetitionOptionDto(
                        c.getId(),
                        c.getCompetitionName(),
                        c.getCountryName(),
                        c.getProviderName(),
                        c.getExternalLeagueId()
                ))
                .toList();
    }
}