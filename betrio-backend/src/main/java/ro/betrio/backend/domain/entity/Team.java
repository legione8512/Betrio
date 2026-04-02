package ro.betrio.backend.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "external_team_id", nullable = false)
    private Long externalTeamId;

    @Column(name = "team_name", nullable = false, length = 150)
    private String teamName;

    @Column(name = "short_code", length = 20)
    private String shortCode;

    @Column(name = "country_name", length = 100)
    private String countryName;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "national_flag", nullable = false)
    private Boolean nationalFlag = false;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "venue_name", length = 150)
    private String venueName;

    @Column(name = "venue_city", length = 150)
    private String venueCity;

    @Column(name = "venue_capacity")
    private Integer venueCapacity;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public Long getExternalTeamId() { return externalTeamId; }
    public void setExternalTeamId(Long externalTeamId) { this.externalTeamId = externalTeamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }
    public Boolean getNationalFlag() { return nationalFlag; }
    public void setNationalFlag(Boolean nationalFlag) { this.nationalFlag = nationalFlag; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public String getVenueCity() { return venueCity; }
    public void setVenueCity(String venueCity) { this.venueCity = venueCity; }
    public Integer getVenueCapacity() { return venueCapacity; }
    public void setVenueCapacity(Integer venueCapacity) { this.venueCapacity = venueCapacity; }
}