package ro.betrio.backend.domain.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "external_player_id", nullable = false)
    private Long externalPlayerId;

    @Column(name = "player_name", nullable = false, length = 150)
    private String playerName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "age")
    private Integer age;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "height_text", length = 30)
    private String heightText;

    @Column(name = "weight_text", length = 30)
    private String weightText;

    @Column(name = "injured_flag", nullable = false)
    private Boolean injuredFlag = false;

    @Column(name = "photo_url")
    private String photoUrl;

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

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public Long getExternalPlayerId() {
		return externalPlayerId;
	}

	public void setExternalPlayerId(Long externalPlayerId) {
		this.externalPlayerId = externalPlayerId;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getHeightText() {
		return heightText;
	}

	public void setHeightText(String heightText) {
		this.heightText = heightText;
	}

	public String getWeightText() {
		return weightText;
	}

	public void setWeightText(String weightText) {
		this.weightText = weightText;
	}

	public Boolean getInjuredFlag() {
		return injuredFlag;
	}

	public void setInjuredFlag(Boolean injuredFlag) {
		this.injuredFlag = injuredFlag;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
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