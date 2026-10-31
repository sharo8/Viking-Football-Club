package rw.vikingfc.club.lineup;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RatingMath {

    private RatingMath() {
    }

    public static BigDecimal system(int minutes, int goals, int assists, int yellow, int red,
                                    boolean cleanSheet, boolean defensive, boolean motm,
                                    BigDecimal base, BigDecimal goalW, BigDecimal assistW,
                                    BigDecimal cleanW, BigDecimal motmW, BigDecimal yellowW, BigDecimal redW) {
        if (minutes < 15) {
            return null;
        }
        double score = n(base, 6) + goals * n(goalW, 1) + assists * n(assistW, 0.7)
                + (cleanSheet && defensive ? n(cleanW, 0.5) : 0)
                + (motm ? n(motmW, 0.5) : 0)
                - yellow * n(yellowW, 0.3)
                - red * n(redW, 2);
        return clamp(score);
    }

    public static BigDecimal blend(BigDecimal coach, BigDecimal system, int coachPct) {
        if (coach == null) {
            return system;
        }
        if (system == null) {
            return coach;
        }
        int pct = Math.max(0, Math.min(100, coachPct));
        double mixed = coach.doubleValue() * pct / 100.0 + system.doubleValue() * (100 - pct) / 100.0;
        return clamp(mixed);
    }

    public static BigDecimal clamp(double score) {
        double bounded = Math.max(1, Math.min(10, score));
        return BigDecimal.valueOf(bounded).setScale(1, RoundingMode.HALF_UP);
    }

    private static double n(BigDecimal value, double fallback) {
        return value == null ? fallback : value.doubleValue();
    }
}
