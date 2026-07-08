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

    public MatchPredictionDto predict(MatchFeatureSnapshotDto features) {

        double homeLambda = features.expectedHomeGoals();
        double awayLambda = features.expectedAwayGoals();

        double homeWin = 0;
        double draw = 0;
        double awayWin = 0;
        double over25 = 0;
        double bttsYes = 0;

        List<ExactScoreDto> exactScores = new ArrayList<>();

        for (int h = 0; h <= 6; h++) {
            for (int a = 0; a <= 6; a++) {
                double p = poisson(h, homeLambda) * poisson(a, awayLambda);
                exactScores.add(new ExactScoreDto(h, a, p));

                if (h > a) {
                    homeWin += p;
                } else if (h == a) {
                    draw += p;
                } else {
                    awayWin += p;
                }

                if ((h + a) >= 3) {
                    over25 += p;
                }
                if (h >= 1 && a >= 1) {
                    bttsYes += p;
                }
            }
        }

        exactScores.sort(Comparator.comparingDouble(ExactScoreDto::probability).reversed());
        List<ExactScoreDto> topFive = exactScores.stream().limit(5).toList();

        return new MatchPredictionDto(
                features.fixtureId(),
                round4(homeWin),
                round4(draw),
                round4(awayWin),
                round4(over25),
                round4(1.0 - over25),
                round4(bttsYes),
                round4(1.0 - bttsYes),
                topFive
        );
    }

    private double poisson(int goals, double lambda) {
        return Math.exp(-lambda) * Math.pow(lambda, goals) / factorial(goals);
    }

    private double factorial(int n) {
        if (n <= 1) {
            return 1.0;
        }
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}