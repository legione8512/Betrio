package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fixture_team")
public class FixtureTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "side", nullable = false, length = 10)
    private String side;

    @Column(name = "winner_flag")
    private Boolean winnerFlag;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public Fixture getFixture() { return fixture; }
    public void setFixture(Fixture fixture) { this.fixture = fixture; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public Boolean getWinnerFlag() { return winnerFlag; }
    public void setWinnerFlag(Boolean winnerFlag) { this.winnerFlag = winnerFlag; }
}