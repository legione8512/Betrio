package ro.betrio.backend.domain.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "season")
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(name = "external_season_year", nullable = false)
    private Integer externalSeasonYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_flag", nullable = false)
    private Boolean currentFlag = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition competition) { this.competition = competition; }
    public Integer getExternalSeasonYear() { return externalSeasonYear; }
    public void setExternalSeasonYear(Integer externalSeasonYear) { this.externalSeasonYear = externalSeasonYear; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Boolean getCurrentFlag() { return currentFlag; }
    public void setCurrentFlag(Boolean currentFlag) { this.currentFlag = currentFlag; }

}