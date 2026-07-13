package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "prediction_evaluation")
public class PredictionEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_run_id", nullable = false, unique = true)
    private PredictionRun predictionRun;

    @Column(name = "evaluated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime evaluatedAt;

    @Column(name = "actual_home_goals", nullable = false)
    private Integer actualHomeGoals;

    @Column(name = "actual_away_goals", nullable = false)
    private Integer actualAwayGoals;

    @Column(name = "predicted_result_code", nullable = false, length = 10)
    private String predictedResultCode;

    @Column(name = "actual_result_code", nullable = false, length = 10)
    private String actualResultCode;

    @Column(name = "actual_over25", nullable = false)
    private Boolean actualOver25;

    @Column(name = "actual_btts_yes", nullable = false)
    private Boolean actualBttsYes;

    @Column(name = "hit_1x2", nullable = false)
    private Boolean hit1x2;

    @Column(name = "hit_over25", nullable = false)
    private Boolean hitOver25;

    @Column(name = "hit_btts", nullable = false)
    private Boolean hitBtts;

    @Column(name = "top_exact_score_hit", nullable = false)
    private Boolean topExactScoreHit;

    @Column(name = "brier_score_1x2", nullable = false)
    private Double brierScore1x2;

    @Column(name = "log_loss_1x2", nullable = false)
    private Double logLoss1x2;

    @Column(name = "market_home_implied_probability")
    private Double marketHomeImpliedProbability;

    @Column(name = "market_draw_implied_probability")
    private Double marketDrawImpliedProbability;

    @Column(name = "market_away_implied_probability")
    private Double marketAwayImpliedProbability;

    @Column(name = "market_home_odd")
    private Double marketHomeOdd;

    @Column(name = "market_draw_odd")
    private Double marketDrawOdd;

    @Column(name = "market_away_odd")
    private Double marketAwayOdd;
    
    @Column(name = "model_edge_home")
    private Double modelEdgeHome;

    @Column(name = "model_edge_draw")
    private Double modelEdgeDraw;

    @Column(name = "model_edge_away")
    private Double modelEdgeAway;

    public Long getId() { return id; }

    public PredictionRun getPredictionRun() { return predictionRun; }
    public void setPredictionRun(PredictionRun predictionRun) { this.predictionRun = predictionRun; }

    public OffsetDateTime getEvaluatedAt() { return evaluatedAt; }

    public Integer getActualHomeGoals() { return actualHomeGoals; }
    public void setActualHomeGoals(Integer actualHomeGoals) { this.actualHomeGoals = actualHomeGoals; }

    public Integer getActualAwayGoals() { return actualAwayGoals; }
    public void setActualAwayGoals(Integer actualAwayGoals) { this.actualAwayGoals = actualAwayGoals; }

    public String getPredictedResultCode() { return predictedResultCode; }
    public void setPredictedResultCode(String predictedResultCode) { this.predictedResultCode = predictedResultCode; }

    public String getActualResultCode() { return actualResultCode; }
    public void setActualResultCode(String actualResultCode) { this.actualResultCode = actualResultCode; }

    public Boolean getActualOver25() { return actualOver25; }
    public void setActualOver25(Boolean actualOver25) { this.actualOver25 = actualOver25; }

    public Boolean getActualBttsYes() { return actualBttsYes; }
    public void setActualBttsYes(Boolean actualBttsYes) { this.actualBttsYes = actualBttsYes; }

    public Boolean getHit1x2() { return hit1x2; }
    public void setHit1x2(Boolean hit1x2) { this.hit1x2 = hit1x2; }

    public Boolean getHitOver25() { return hitOver25; }
    public void setHitOver25(Boolean hitOver25) { this.hitOver25 = hitOver25; }

    public Boolean getHitBtts() { return hitBtts; }
    public void setHitBtts(Boolean hitBtts) { this.hitBtts = hitBtts; }

    public Boolean getTopExactScoreHit() { return topExactScoreHit; }
    public void setTopExactScoreHit(Boolean topExactScoreHit) { this.topExactScoreHit = topExactScoreHit; }

    public Double getBrierScore1x2() { return brierScore1x2; }
    public void setBrierScore1x2(Double brierScore1x2) { this.brierScore1x2 = brierScore1x2; }

    public Double getLogLoss1x2() { return logLoss1x2; }
    public void setLogLoss1x2(Double logLoss1x2) { this.logLoss1x2 = logLoss1x2; }

    public Double getMarketHomeImpliedProbability() { return marketHomeImpliedProbability; }
    public void setMarketHomeImpliedProbability(Double marketHomeImpliedProbability) { this.marketHomeImpliedProbability = marketHomeImpliedProbability; }

    public Double getMarketDrawImpliedProbability() { return marketDrawImpliedProbability; }
    public void setMarketDrawImpliedProbability(Double marketDrawImpliedProbability) { this.marketDrawImpliedProbability = marketDrawImpliedProbability; }

    public Double getMarketAwayImpliedProbability() { return marketAwayImpliedProbability; }
    public void setMarketAwayImpliedProbability(Double marketAwayImpliedProbability) { this.marketAwayImpliedProbability = marketAwayImpliedProbability; }

    public Double getMarketHomeOdd() { return marketHomeOdd; }
    public void setMarketHomeOdd(Double marketHomeOdd) { this.marketHomeOdd = marketHomeOdd; }

    public Double getMarketDrawOdd() { return marketDrawOdd; }
    public void setMarketDrawOdd(Double marketDrawOdd) { this.marketDrawOdd = marketDrawOdd; }

    public Double getMarketAwayOdd() { return marketAwayOdd; }
    public void setMarketAwayOdd(Double marketAwayOdd) { this.marketAwayOdd = marketAwayOdd; }
    
    public Double getModelEdgeHome() { return modelEdgeHome; }
    public void setModelEdgeHome(Double modelEdgeHome) { this.modelEdgeHome = modelEdgeHome; }

    public Double getModelEdgeDraw() { return modelEdgeDraw; }
    public void setModelEdgeDraw(Double modelEdgeDraw) { this.modelEdgeDraw = modelEdgeDraw; }

    public Double getModelEdgeAway() { return modelEdgeAway; }
    public void setModelEdgeAway(Double modelEdgeAway) { this.modelEdgeAway = modelEdgeAway; }
}