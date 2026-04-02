package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "prediction_run")
public class PredictionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "generated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "expected_home_goals")
    private Double expectedHomeGoals;

    @Column(name = "expected_away_goals")
    private Double expectedAwayGoals;

    @Column(name = "home_win_probability", nullable = false)
    private Double homeWinProbability;

    @Column(name = "draw_probability", nullable = false)
    private Double drawProbability;

    @Column(name = "away_win_probability", nullable = false)
    private Double awayWinProbability;

    @Column(name = "over25_probability", nullable = false)
    private Double over25Probability;

    @Column(name = "under25_probability", nullable = false)
    private Double under25Probability;

    @Column(name = "btts_yes_probability", nullable = false)
    private Double bttsYesProbability;

    @Column(name = "btts_no_probability", nullable = false)
    private Double bttsNoProbability;

    public Long getId() { return id; }
    public Fixture getFixture() { return fixture; }
    public void setFixture(Fixture fixture) { this.fixture = fixture; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public Double getExpectedHomeGoals() { return expectedHomeGoals; }
    public void setExpectedHomeGoals(Double expectedHomeGoals) { this.expectedHomeGoals = expectedHomeGoals; }
    public Double getExpectedAwayGoals() { return expectedAwayGoals; }
    public void setExpectedAwayGoals(Double expectedAwayGoals) { this.expectedAwayGoals = expectedAwayGoals; }
    public Double getHomeWinProbability() { return homeWinProbability; }
    public void setHomeWinProbability(Double homeWinProbability) { this.homeWinProbability = homeWinProbability; }
    public Double getDrawProbability() { return drawProbability; }
    public void setDrawProbability(Double drawProbability) { this.drawProbability = drawProbability; }
    public Double getAwayWinProbability() { return awayWinProbability; }
    public void setAwayWinProbability(Double awayWinProbability) { this.awayWinProbability = awayWinProbability; }
    public Double getOver25Probability() { return over25Probability; }
    public void setOver25Probability(Double over25Probability) { this.over25Probability = over25Probability; }
    public Double getUnder25Probability() { return under25Probability; }
    public void setUnder25Probability(Double under25Probability) { this.under25Probability = under25Probability; }
    public Double getBttsYesProbability() { return bttsYesProbability; }
    public void setBttsYesProbability(Double bttsYesProbability) { this.bttsYesProbability = bttsYesProbability; }
    public Double getBttsNoProbability() { return bttsNoProbability; }
    public void setBttsNoProbability(Double bttsNoProbability) { this.bttsNoProbability = bttsNoProbability; }
}