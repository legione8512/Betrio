package ro.betrio.backend.service.prediction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import ro.betrio.backend.api.dto.ExactScoreDto;
import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.MatchPredictionDto;

@Service
public class ProbabilityEngineService {

    private static final double SCORE_GRID_EPSILON = 1e-9;
    private static final int MAX_GOALS = 40;

    public MatchPredictionDto predict(MatchFeatureSnapshotDto features) {
        double homeLambda = sanitizeLambda(features.expectedHomeGoals());
        double awayLambda = sanitizeLambda(features.expectedAwayGoals());

        int homeGoalLimit = goalLimit(homeLambda);
        int awayGoalLimit = goalLimit(awayLambda);

        List<Double> homeGoalProbabilities = poissonDistribution(homeLambda, homeGoalLimit);
        List<Double> awayGoalProbabilities = poissonDistribution(awayLambda, awayGoalLimit);

        double homeWin = 0.0;
        double draw = 0.0;
        double awayWin = 0.0;
        double scoreGridMass = 0.0;

        List<ExactScoreDto> exactScores = new ArrayList<>();

        for (int h = 0; h <= homeGoalLimit; h++) {
            for (int a = 0; a <= awayGoalLimit; a++) {
                double probability = homeGoalProbabilities.get(h) * awayGoalProbabilities.get(a);
                scoreGridMass += probability;

                exactScores.add(new ExactScoreDto(h, a, probability));

                if (h > a) {
                    homeWin += probability;
                } else if (h == a) {
                    draw += probability;
                } else {
                    awayWin += probability;
                }
            }
        }

        if (scoreGridMass > 0.0 && Double.isFinite(scoreGridMass)) {
            homeWin = homeWin / scoreGridMass;
            draw = draw / scoreGridMass;
            awayWin = awayWin / scoreGridMass;
        }

        double totalGoalsLambda = homeLambda + awayLambda;

        double under25 = poissonCdf(2, totalGoalsLambda);
        double over25 = 1.0 - under25;

        double bttsYes = (1.0 - Math.exp(-homeLambda)) * (1.0 - Math.exp(-awayLambda));
        double bttsNo = 1.0 - bttsYes;

        exactScores.sort(Comparator.comparingDouble(ExactScoreDto::probability).reversed());
        List<ExactScoreDto> topFive = exactScores.stream()
                .limit(5)
                .toList();

        return new MatchPredictionDto(
                features.fixtureId(),
                round4Probability(homeWin),
                round4Probability(draw),
                round4Probability(awayWin),
                round4Probability(over25),
                round4Probability(under25),
                round4Probability(bttsYes),
                round4Probability(bttsNo),
                topFive
        );
    }

    private int goalLimit(double lambda) {
        if (lambda <= 0.0) {
            return 0;
        }

        double probability = Math.exp(-lambda);
        double cumulativeProbability = probability;
        int goals = 0;

        while ((1.0 - cumulativeProbability) > SCORE_GRID_EPSILON && goals < MAX_GOALS) {
            goals++;
            probability = probability * lambda / goals;
            cumulativeProbability += probability;
        }

        return goals;
    }

    private List<Double> poissonDistribution(double lambda, int maxGoals) {
        List<Double> probabilities = new ArrayList<>(maxGoals + 1);

        double probability = Math.exp(-lambda);
        probabilities.add(probability);

        for (int goals = 1; goals <= maxGoals; goals++) {
            probability = probability * lambda / goals;
            probabilities.add(probability);
        }

        return probabilities;
    }

    private double poissonCdf(int maxGoals, double lambda) {
        if (maxGoals < 0) {
            return 0.0;
        }

        if (lambda <= 0.0) {
            return 1.0;
        }

        double probability = Math.exp(-lambda);
        double cumulativeProbability = probability;

        for (int goals = 1; goals <= maxGoals; goals++) {
            probability = probability * lambda / goals;
            cumulativeProbability += probability;
        }

        return clamp01(cumulativeProbability);
    }

    private double sanitizeLambda(double lambda) {
        if (!Double.isFinite(lambda) || lambda < 0.0) {
            return 0.0;
        }

        return lambda;
    }

    private double round4Probability(double value) {
        return round4(clamp01(value));
    }

    private double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }

        if (value < 0.0) {
            return 0.0;
        }

        if (value > 1.0) {
            return 1.0;
        }

        return value;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}