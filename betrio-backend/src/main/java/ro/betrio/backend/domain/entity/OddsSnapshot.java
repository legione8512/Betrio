package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "odds_snapshot")
public class OddsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @Column(name = "bookmaker_id")
    private Long bookmakerId;

    @Column(name = "bookmaker_name", length = 150)
    private String bookmakerName;

    @Column(name = "market_name", nullable = false, length = 150)
    private String marketName;

    @Column(name = "outcome_name", nullable = false, length = 150)
    private String outcomeName;

    @Column(name = "odd_value")
    private Double oddValue;

    @Column(name = "captured_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime capturedAt;

    public Long getId() { return id; }
    public Fixture getFixture() { return fixture; }
    public void setFixture(Fixture fixture) { this.fixture = fixture; }
    public Long getBookmakerId() { return bookmakerId; }
    public void setBookmakerId(Long bookmakerId) { this.bookmakerId = bookmakerId; }
    public String getBookmakerName() { return bookmakerName; }
    public void setBookmakerName(String bookmakerName) { this.bookmakerName = bookmakerName; }
    public String getMarketName() { return marketName; }
    public void setMarketName(String marketName) { this.marketName = marketName; }
    public String getOutcomeName() { return outcomeName; }
    public void setOutcomeName(String outcomeName) { this.outcomeName = outcomeName; }
    public Double getOddValue() { return oddValue; }
    public void setOddValue(Double oddValue) { this.oddValue = oddValue; }
    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }
}