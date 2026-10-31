package rw.vikingfc.club.attendance;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AttendanceCalculator {

    private AttendanceCalculator() {
    }

    public static BigDecimal percentage(int present, int late, int excused, int absent) {
        int denominator = present + late + excused + absent;
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(present + late)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    public static String band(BigDecimal percentage, int onTrack, int watch) {
        if (percentage.compareTo(BigDecimal.valueOf(onTrack)) >= 0) {
            return "On track";
        }
        if (percentage.compareTo(BigDecimal.valueOf(watch)) >= 0) {
            return "Watch";
        }
        return "At risk";
    }
}
