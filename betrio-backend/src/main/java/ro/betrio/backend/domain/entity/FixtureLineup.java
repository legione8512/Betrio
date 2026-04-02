package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fixture_lineup")
public class FixtureLineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "lineup_type", nullable = false, length = 20)
    private String lineupType;

    @Column(name = "player_name", nullable = false, length = 150)
    private String playerName;

    @Column(name = "shirt_number")
    private Integer shirtNumber;

    @Column(name = "position_code", length = 20)
    private String positionCode;

    @Column(name = "grid_coordinate", length = 20)
    private String gridCoordinate;

    @Column(name = "captain_flag", nullable = false)
    private Boolean captainFlag = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Fixture getFixture() {
		return fixture;
	}

	public void setFixture(Fixture fixture) {
		this.fixture = fixture;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public String getLineupType() {
		return lineupType;
	}

	public void setLineupType(String lineupType) {
		this.lineupType = lineupType;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public Integer getShirtNumber() {
		return shirtNumber;
	}

	public void setShirtNumber(Integer shirtNumber) {
		this.shirtNumber = shirtNumber;
	}

	public String getPositionCode() {
		return positionCode;
	}

	public void setPositionCode(String positionCode) {
		this.positionCode = positionCode;
	}

	public String getGridCoordinate() {
		return gridCoordinate;
	}

	public void setGridCoordinate(String gridCoordinate) {
		this.gridCoordinate = gridCoordinate;
	}

	public Boolean getCaptainFlag() {
		return captainFlag;
	}

	public void setCaptainFlag(Boolean captainFlag) {
		this.captainFlag = captainFlag;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}