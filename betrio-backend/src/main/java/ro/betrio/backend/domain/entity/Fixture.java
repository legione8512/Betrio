package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fixture")
public class Fixture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "external_fixture_id", nullable = false)
    private Long externalFixtureId;

    @Column(name = "referee_name", length = 150)
    private String refereeName;

    @Column(name = "timezone_name", length = 100)
    private String timezoneName;

    @Column(name = "kickoff_at", nullable = false)
    private OffsetDateTime kickoffAt;

    @Column(name = "status_long", length = 100)
    private String statusLong;

    @Column(name = "status_short", length = 20)
    private String statusShort;

    @Column(name = "elapsed_minutes")
    private Integer elapsedMinutes;

    @Column(name = "league_round", length = 100)
    private String leagueRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @Column(name = "home_goals")
    private Integer homeGoals;

    @Column(name = "away_goals")
    private Integer awayGoals;

    @Column(name = "halftime_home_goals")
    private Integer halftimeHomeGoals;

    @Column(name = "halftime_away_goals")
    private Integer halftimeAwayGoals;

    @Column(name = "fulltime_home_goals")
    private Integer fulltimeHomeGoals;

    @Column(name = "fulltime_away_goals")
    private Integer fulltimeAwayGoals;

    @Column(name = "extratime_home_goals")
    private Integer extratimeHomeGoals;

    @Column(name = "extratime_away_goals")
    private Integer extratimeAwayGoals;

    @Column(name = "penalty_home_goals")
    private Integer penaltyHomeGoals;

    @Column(name = "penalty_away_goals")
    private Integer penaltyAwayGoals;

    @Column(name = "venue_name", length = 150)
    private String venueName;

    @Column(name = "venue_city", length = 150)
    private String venueCity;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public Long getExternalFixtureId() { return externalFixtureId; }
    public void setExternalFixtureId(Long externalFixtureId) { this.externalFixtureId = externalFixtureId; }
    public String getRefereeName() { return refereeName; }
    public void setRefereeName(String refereeName) { this.refereeName = refereeName; }
    public String getTimezoneName() { return timezoneName; }
    public void setTimezoneName(String timezoneName) { this.timezoneName = timezoneName; }
    public OffsetDateTime getKickoffAt() { return kickoffAt; }
    public void setKickoffAt(OffsetDateTime kickoffAt) { this.kickoffAt = kickoffAt; }
    public String getStatusLong() { return statusLong; }
    public void setStatusLong(String statusLong) { this.statusLong = statusLong; }
    public String getStatusShort() { return statusShort; }
    public void setStatusShort(String statusShort) { this.statusShort = statusShort; }
    public Integer getElapsedMinutes() { return elapsedMinutes; }
    public void setElapsedMinutes(Integer elapsedMinutes) { this.elapsedMinutes = elapsedMinutes; }
    public String getLeagueRound() { return leagueRound; }
    public void setLeagueRound(String leagueRound) { this.leagueRound = leagueRound; }
    public Team getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }
    public Integer getHomeGoals() { return homeGoals; }
    public void setHomeGoals(Integer homeGoals) { this.homeGoals = homeGoals; }
    public Integer getAwayGoals() { return awayGoals; }
    public void setAwayGoals(Integer awayGoals) { this.awayGoals = awayGoals; }
    public Integer getHalftimeHomeGoals() { return halftimeHomeGoals; }
    public void setHalftimeHomeGoals(Integer halftimeHomeGoals) { this.halftimeHomeGoals = halftimeHomeGoals; }
    public Integer getHalftimeAwayGoals() { return halftimeAwayGoals; }
    public void setHalftimeAwayGoals(Integer halftimeAwayGoals) { this.halftimeAwayGoals = halftimeAwayGoals; }
    public Integer getFulltimeHomeGoals() { return fulltimeHomeGoals; }
    public void setFulltimeHomeGoals(Integer fulltimeHomeGoals) { this.fulltimeHomeGoals = fulltimeHomeGoals; }
    public Integer getFulltimeAwayGoals() { return fulltimeAwayGoals; }
    public void setFulltimeAwayGoals(Integer fulltimeAwayGoals) { this.fulltimeAwayGoals = fulltimeAwayGoals; }
    public Integer getExtratimeHomeGoals() { return extratimeHomeGoals; }
    public void setExtratimeHomeGoals(Integer extratimeHomeGoals) { this.extratimeHomeGoals = extratimeHomeGoals; }
    public Integer getExtratimeAwayGoals() { return extratimeAwayGoals; }
    public void setExtratimeAwayGoals(Integer extratimeAwayGoals) { this.extratimeAwayGoals = extratimeAwayGoals; }
    public Integer getPenaltyHomeGoals() { return penaltyHomeGoals; }
    public void setPenaltyHomeGoals(Integer penaltyHomeGoals) { this.penaltyHomeGoals = penaltyHomeGoals; }
    public Integer getPenaltyAwayGoals() { return penaltyAwayGoals; }
    public void setPenaltyAwayGoals(Integer penaltyAwayGoals) { this.penaltyAwayGoals = penaltyAwayGoals; }
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public String getVenueCity() { return venueCity; }
    public void setVenueCity(String venueCity) { this.venueCity = venueCity; }

}