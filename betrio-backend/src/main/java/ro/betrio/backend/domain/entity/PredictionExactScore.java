package ro.betrio.backend.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "prediction_exact_score")
public class PredictionExactScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_run_id", nullable = false)
    private PredictionRun predictionRun;

    @Column(name = "home_goals", nullable = false)
    private Integer homeGoals;

    @Column(name = "away_goals", nullable = false)
    private Integer awayGoals;

    @Column(name = "probability", nullable = false)
    private Double probability;

    public Long getId() { return id; }
    public PredictionRun getPredictionRun() { return predictionRun; }
    public void setPredictionRun(PredictionRun predictionRun) { this.predictionRun = predictionRun; }
    public Integer getHomeGoals() { return homeGoals; }
    public void setHomeGoals(Integer homeGoals) { this.homeGoals = homeGoals; }
    public Integer getAwayGoals() { return awayGoals; }
    public void setAwayGoals(Integer awayGoals) { this.awayGoals = awayGoals; }
    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }
}