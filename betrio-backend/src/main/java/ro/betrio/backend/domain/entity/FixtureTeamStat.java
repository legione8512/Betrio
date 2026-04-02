package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fixture_team_stat")
public class FixtureTeamStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "stat_name", nullable = false, length = 100)
    private String statName;

    @Column(name = "stat_value_text", length = 100)
    private String statValueText;

    @Column(name = "stat_value_number")
    private Double statValueNumber;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }

    public Fixture getFixture() { return fixture; }
    public void setFixture(Fixture fixture) { this.fixture = fixture; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public String getStatName() { return statName; }
    public void setStatName(String statName) { this.statName = statName; }

    public String getStatValueText() { return statValueText; }
    public void setStatValueText(String statValueText) { this.statValueText = statValueText; }

    public Double getStatValueNumber() { return statValueNumber; }
    public void setStatValueNumber(Double statValueNumber) { this.statValueNumber = statValueNumber; }
}