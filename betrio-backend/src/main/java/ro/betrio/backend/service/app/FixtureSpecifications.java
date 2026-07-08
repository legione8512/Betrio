package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import ro.betrio.backend.domain.entity.Fixture;

public final class FixtureSpecifications {

    private FixtureSpecifications() {
    }

    public static Specification<Fixture> queryContains(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + query.trim().toLowerCase() + "%";

            var homeTeam = root.join("homeTeam", JoinType.LEFT);
            var awayTeam = root.join("awayTeam", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(homeTeam.get("teamName")), pattern),
                    cb.like(cb.lower(awayTeam.get("teamName")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("leagueRound"), "")), pattern)
            );
        };
    }

    public static Specification<Fixture> hasStatus(String status) {
        return (root, cq, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("statusShort"), status);
        };
    }

    public static Specification<Fixture> hasTeamId(Long teamId) {
        return (root, cq, cb) -> {
            if (teamId == null) {
                return cb.conjunction();
            }
            return cb.or(
                    cb.equal(root.get("homeTeam").get("externalTeamId"), teamId),
                    cb.equal(root.get("awayTeam").get("externalTeamId"), teamId)
            );
        };
    }

    public static Specification<Fixture> hasRound(String round) {
        return (root, cq, cb) -> {
            if (round == null || round.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("leagueRound"), round);
        };
    }

    public static Specification<Fixture> kickoffFrom(OffsetDateTime from) {
        return (root, cq, cb) -> {
            if (from == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("kickoffAt"), from);
        };
    }

    public static Specification<Fixture> kickoffTo(OffsetDateTime to) {
        return (root, cq, cb) -> {
            if (to == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("kickoffAt"), to);
        };
    }
    public static Specification<Fixture> hasCompetitionId(Long competitionId) {
        return (root, query, cb) -> {
            if (competitionId == null) {
                return null;
            }
            return cb.equal(root.get("season").get("competition").get("id"), competitionId);
        };
    }
}