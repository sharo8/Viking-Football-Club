package rw.vikingfc.club.common.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class ClubDates {

    private ClubDates() {
    }

    public static int weekNumber(LocalDate firstMonday, LocalDate date) {
        if (date.isBefore(firstMonday)) {
            return 1;
        }
        return (int) ChronoUnit.WEEKS.between(firstMonday, date) + 1;
    }

    public static int remainingMonths(LocalDate today, LocalDate endDate) {
        if (endDate.isBefore(today)) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(today, endDate);
    }
}
