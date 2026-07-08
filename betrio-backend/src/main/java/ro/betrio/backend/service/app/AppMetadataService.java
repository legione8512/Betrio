package ro.betrio.backend.service.app;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.meta.AppDashboardSummaryDto;
import ro.betrio.backend.api.dto.meta.FilterValueDto;
import ro.betrio.backend.api.dto.meta.TeamOptionDto;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.PredictionEvaluationRepository;
import ro.betrio.backend.repository.TeamRepository;

@Service
public class AppMetadataService {

    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;
    private final PredictionEvaluationRepository predictionEvaluationRepository;

    public AppMetadataService(
            FixtureRepository fixtureRepository,
            TeamRepository teamRepository,
            PredictionEvaluationRepository predictionEvaluationRepository) {
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
        this.predictionEvaluationRepository = predictionEvaluationRepository;
    }

    @Transactional(readOnly = true)
    public List<FilterValueDto> getStatuses() {
        return fixtureRepository.findDistinctStatuses()
                .stream()
                .map(value -> new FilterValueDto(value, value))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FilterValueDto> getRounds(Long competitionId) {
        List<String> rounds = competitionId == null
                ? fixtureRepository.findDistinctRounds()
                : fixtureRepository.findDistinctRoundsByCompetition(competitionId);

        return rounds.stream()
                .map(value -> new FilterValueDto(value, value))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamOptionDto> getTeams(Long competitionId) {
        List<Team> rawTeams = competitionId == null
                ? teamRepository.findAllOrderedByTeamName()
                : teamRepository.findAllByCompetitionOrderedByTeamName(competitionId);

        Map<Long, TeamOptionDto> uniqueByExternalTeamId = new LinkedHashMap<>();

        for (Team team : rawTeams) {
            uniqueByExternalTeamId.putIfAbsent(
                    team.getExternalTeamId(),
                    new TeamOptionDto(team.getExternalTeamId(), team.getTeamName())
            );
        }

        return uniqueByExternalTeamId.values()
                .stream()
                .toList();
    }

    @Transactional(readOnly = true)
    public AppDashboardSummaryDto getDashboardSummary() {
        return new AppDashboardSummaryDto(
                fixtureRepository.count(),
                fixtureRepository.countUpcomingFixtures(),
                fixtureRepository.countFinishedFixtures(),
                predictionEvaluationRepository.count()
        );
    }
}