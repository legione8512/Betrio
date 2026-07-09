package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto;
import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto.EdgeSummary;
import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto.MarketOdds;
import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto.ModelProbabilities;
import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto.OddsMovement;
import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto.SelectionMovement;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.OddsSnapshot;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.OddsSnapshotRepository;
import ro.betrio.backend.repository.PredictionRunRepository;

@Service
public class MarketComparisonService {

    private final FixtureRepository fixtureRepository;
    private final PredictionRunRepository predictionRunRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;

    public MarketComparisonService(
            FixtureRepository fixtureRepository,
            PredictionRunRepository predictionRunRepository,
            OddsSnapshotRepository oddsSnapshotRepository) {

        this.fixtureRepository = fixtureRepository;
        this.predictionRunRepository = predictionRunRepository;
        this.oddsSnapshotRepository = oddsSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public FixtureMarketComparisonDto getMarketComparison(Long fixtureId) {
        Fixture fixture = fixtureRepository.findDetailedById(fixtureId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Fixture not found: " + fixtureId));

        PredictionRun latestPrediction = predictionRunRepository
                .findTopByFixtureIdOrderByGeneratedAtDesc(fixtureId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No prediction run found for fixture: "
                                        + fixtureId));

        List<OddsSnapshot> allSnapshots =
                oddsSnapshotRepository
                        .findByFixtureIdOrderByCapturedAtDesc(fixtureId);

        return buildMarketComparison(
                fixture,
                latestPrediction,
                allSnapshots
        );
    }

    public FixtureMarketComparisonDto buildMarketComparison(
            Fixture fixture,
            PredictionRun latestPrediction,
            List<OddsSnapshot> allSnapshots) {

        List<OddsSnapshot> safeSnapshots =
                allSnapshots == null ? List.of() : allSnapshots;

        OddsMovement oddsMovement =
                buildOddsMovement(fixture, safeSnapshots);

        ModelProbabilities modelProbabilities =
                new ModelProbabilities(
                        latestPrediction.getHomeWinProbability(),
                        latestPrediction.getDrawProbability(),
                        latestPrediction.getAwayWinProbability()
                );

        if (safeSnapshots.isEmpty()) {
            return new FixtureMarketComparisonDto(
                    fixture.getId(),
                    fixture.getHomeTeam().getTeamName(),
                    fixture.getAwayTeam().getTeamName(),
                    fixture.getKickoffAt(),
                    modelProbabilities,
                    null,
                    null,
                    null,
                    null,
                    oddsMovement
            );
        }

        OffsetDateTime latestCapturedAt =
                safeSnapshots.get(0).getCapturedAt();

        List<OddsSnapshot> latestBatch = safeSnapshots.stream()
                .filter(snapshot ->
                        latestCapturedAt != null
                                && latestCapturedAt.equals(snapshot.getCapturedAt()))
                .filter(this::isOneXTwoMarket)
                .toList();

        Map<String, OddsSnapshot> selectionMap = latestBatch.stream()
                .collect(Collectors.toMap(
                        this::normalizeOutcome,
                        snapshot -> snapshot,
                        (first, duplicate) -> first
                ));

        OddsSnapshot homeSnapshot = selectionMap.get("HOME");
        OddsSnapshot drawSnapshot = selectionMap.get("DRAW");
        OddsSnapshot awaySnapshot = selectionMap.get("AWAY");

        if (homeSnapshot == null
                || drawSnapshot == null
                || awaySnapshot == null
                || homeSnapshot.getOddValue() == null
                || drawSnapshot.getOddValue() == null
                || awaySnapshot.getOddValue() == null
                || homeSnapshot.getOddValue() <= 0.0
                || drawSnapshot.getOddValue() <= 0.0
                || awaySnapshot.getOddValue() <= 0.0) {

            return new FixtureMarketComparisonDto(
                    fixture.getId(),
                    fixture.getHomeTeam().getTeamName(),
                    fixture.getAwayTeam().getTeamName(),
                    fixture.getKickoffAt(),
                    modelProbabilities,
                    null,
                    null,
                    null,
                    latestCapturedAt,
                    oddsMovement
            );
        }

        double homeOdds = homeSnapshot.getOddValue();
        double drawOdds = drawSnapshot.getOddValue();
        double awayOdds = awaySnapshot.getOddValue();

        double rawHome = 1.0 / homeOdds;
        double rawDraw = 1.0 / drawOdds;
        double rawAway = 1.0 / awayOdds;

        double sum = rawHome + rawDraw + rawAway;

        double normalizedHome = rawHome / sum;
        double normalizedDraw = rawDraw / sum;
        double normalizedAway = rawAway / sum;

        double modelHome =
                latestPrediction.getHomeWinProbability();

        double modelDraw =
                latestPrediction.getDrawProbability();

        double modelAway =
                latestPrediction.getAwayWinProbability();

        double edgeHome = modelHome - normalizedHome;
        double edgeDraw = modelDraw - normalizedDraw;
        double edgeAway = modelAway - normalizedAway;

        String bestEdgeSelection =
                bestEdge(edgeHome, edgeDraw, edgeAway);

        return new FixtureMarketComparisonDto(
                fixture.getId(),
                fixture.getHomeTeam().getTeamName(),
                fixture.getAwayTeam().getTeamName(),
                fixture.getKickoffAt(),
                modelProbabilities,
                new MarketOdds(
                        homeOdds,
                        drawOdds,
                        awayOdds,
                        rawHome,
                        rawDraw,
                        rawAway,
                        normalizedHome,
                        normalizedDraw,
                        normalizedAway
                ),
                new EdgeSummary(
                        edgeHome,
                        edgeDraw,
                        edgeAway
                ),
                bestEdgeSelection,
                latestCapturedAt,
                oddsMovement
        );
    }

    private OddsMovement buildOddsMovement(
            Fixture fixture,
            List<OddsSnapshot> odds) {

        if (odds == null || odds.isEmpty()) {
            return unavailableOddsMovement(
                    "Nu avem cote capturate pentru acest fixture."
            );
        }

        List<MarketPoint> points =
                buildCompleteMarketPoints(fixture, odds);

        if (points.size() < 2) {
            return unavailableOddsMovement(
                    "Avem nevoie de cel puțin două capturi complete de cote pentru odds movement."
            );
        }

        MarketPoint first = points.get(0);
        MarketPoint latest = points.get(points.size() - 1);

        SelectionMovement home =
                selectionMovement(
                        "HOME",
                        first.homeOdd(),
                        latest.homeOdd(),
                        first.homeProbability(),
                        latest.homeProbability()
                );

        SelectionMovement draw =
                selectionMovement(
                        "DRAW",
                        first.drawOdd(),
                        latest.drawOdd(),
                        first.drawProbability(),
                        latest.drawProbability()
                );

        SelectionMovement away =
                selectionMovement(
                        "AWAY",
                        first.awayOdd(),
                        latest.awayOdd(),
                        first.awayProbability(),
                        latest.awayProbability()
                );

        SelectionMovement strongest =
                strongestMove(home, draw, away);

        return new OddsMovement(
                true,
                first.capturedAt().toString(),
                latest.capturedAt().toString(),
                home,
                draw,
                away,
                strongest.selection(),
                strongest.direction(),
                strongest.impliedProbabilityDelta(),
                oddsMovementSummary(strongest)
        );
    }

    private List<MarketPoint> buildCompleteMarketPoints(
            Fixture fixture,
            List<OddsSnapshot> odds) {

        Map<OffsetDateTime, MarketPointBuilder> byCapturedAt =
                new HashMap<>();

        for (OddsSnapshot snapshot : odds) {
            if (!isOneXTwoMarket(snapshot)) {
                continue;
            }

            String outcome =
                    normalizeOutcome(snapshot);

            if (outcome == null
                    || "OTHER".equals(outcome)
                    || snapshot.getCapturedAt() == null
                    || snapshot.getOddValue() == null
                    || snapshot.getOddValue() <= 0.0) {
                continue;
            }

            MarketPointBuilder builder =
                    byCapturedAt.computeIfAbsent(
                            snapshot.getCapturedAt(),
                            ignored -> new MarketPointBuilder(
                                    snapshot.getCapturedAt()
                            )
                    );

            if ("HOME".equals(outcome)) {
                builder.homeOdd = snapshot.getOddValue();
            } else if ("DRAW".equals(outcome)) {
                builder.drawOdd = snapshot.getOddValue();
            } else if ("AWAY".equals(outcome)) {
                builder.awayOdd = snapshot.getOddValue();
            }
        }

        return byCapturedAt.values()
                .stream()
                .filter(MarketPointBuilder::isComplete)
                .map(MarketPointBuilder::build)
                .sorted(Comparator.comparing(MarketPoint::capturedAt))
                .toList();
    }

    private SelectionMovement selectionMovement(
            String selection,
            Double firstOdd,
            Double latestOdd,
            Double firstProbability,
            Double latestProbability) {

        double probabilityDelta =
                latestProbability - firstProbability;

        String direction;

        if (probabilityDelta >= 0.01) {
            direction = "STEAM_IN";
        } else if (probabilityDelta <= -0.01) {
            direction = "DRIFT_OUT";
        } else {
            direction = "STABLE";
        }

        return new SelectionMovement(
                selection,
                firstOdd,
                latestOdd,
                latestOdd - firstOdd,
                firstProbability,
                latestProbability,
                probabilityDelta,
                direction
        );
    }

    private SelectionMovement strongestMove(
            SelectionMovement home,
            SelectionMovement draw,
            SelectionMovement away) {

        List<SelectionMovement> movements =
                new ArrayList<>();

        movements.add(home);
        movements.add(draw);
        movements.add(away);

        return movements.stream()
                .max(Comparator.comparingDouble(
                        movement ->
                                Math.abs(
                                        movement.impliedProbabilityDelta()
                                )
                ))
                .orElse(home);
    }

    private String oddsMovementSummary(SelectionMovement strongest) {
        if (strongest == null) {
            return "Nu există mișcare relevantă de cote.";
        }

        String selection = strongest.selection();
        String direction = strongest.direction();

        if ("STEAM_IN".equals(direction)) {
            return "Piața a intrat pe selecția "
                    + selection
                    + ". Probabilitatea implicită a crescut.";
        }

        if ("DRIFT_OUT".equals(direction)) {
            return "Piața s-a îndepărtat de selecția "
                    + selection
                    + ". Probabilitatea implicită a scăzut.";
        }

        return "Cotele sunt relativ stabile. Nu există mișcare puternică de piață.";
    }

    private OddsMovement unavailableOddsMovement(String summary) {
        return new OddsMovement(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                summary
        );
    }

    private boolean isOneXTwoMarket(OddsSnapshot snapshot) {
        String marketName = safe(snapshot.getMarketName());

        return marketName.contains("match winner")
                || marketName.equals("1x2")
                || marketName.equals("match result");
    }

    private String normalizeOutcome(OddsSnapshot snapshot) {
        String outcome = safe(snapshot.getOutcomeName());

        if (outcome.equals("1") || outcome.equals("home")) {
            return "HOME";
        }

        if (outcome.equals("x")
                || outcome.equals("draw")
                || outcome.equals("tie")) {

            return "DRAW";
        }

        if (outcome.equals("2") || outcome.equals("away")) {
            return "AWAY";
        }

        return "OTHER";
    }

    private String bestEdge(
            double home,
            double draw,
            double away) {

        double max = Math.max(home, Math.max(draw, away));

        if (max <= 0.0) {
            return null;
        }

        if (max == home) {
            return "HOME";
        }

        if (max == away) {
            return "AWAY";
        }

        return "DRAW";
    }

    private String safe(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private record MarketPoint(
            OffsetDateTime capturedAt,
            Double homeOdd,
            Double drawOdd,
            Double awayOdd,
            Double homeProbability,
            Double drawProbability,
            Double awayProbability
    ) {
    }

    private static class MarketPointBuilder {

        private final OffsetDateTime capturedAt;
        private Double homeOdd;
        private Double drawOdd;
        private Double awayOdd;

        private MarketPointBuilder(OffsetDateTime capturedAt) {
            this.capturedAt = capturedAt;
        }

        private boolean isComplete() {
            return homeOdd != null
                    && drawOdd != null
                    && awayOdd != null;
        }

        private MarketPoint build() {
            double homeRaw = 1.0 / homeOdd;
            double drawRaw = 1.0 / drawOdd;
            double awayRaw = 1.0 / awayOdd;

            double total = homeRaw + drawRaw + awayRaw;

            return new MarketPoint(
                    capturedAt,
                    homeOdd,
                    drawOdd,
                    awayOdd,
                    homeRaw / total,
                    drawRaw / total,
                    awayRaw / total
            );
        }
    }
}