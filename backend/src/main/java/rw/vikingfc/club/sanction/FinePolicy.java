package rw.vikingfc.club.sanction;

import rw.vikingfc.club.attendance.AttendanceMark;
import rw.vikingfc.club.common.util.Money;

import java.math.BigDecimal;

public final class FinePolicy {

    private FinePolicy() {
    }

    public record Decision(boolean fine, FineType type, BigDecimal amount) {
        public static Decision none() {
            return new Decision(false, null, Money.zero());
        }
    }

    public static Decision training(AttendanceMark mark, Integer minutesLate, boolean justified,
                                    int lateAfterMinutes, BigDecimal lateAmount, BigDecimal absenceAmount) {
        if (mark == null || mark == AttendanceMark.E || mark == AttendanceMark.I || mark == AttendanceMark.N || justified) {
            return Decision.none();
        }
        if (mark == AttendanceMark.A) {
            return new Decision(true, FineType.TRAINING_ABSENCE, Money.nz(absenceAmount));
        }
        if (mark == AttendanceMark.L && minutesLate != null && minutesLate > lateAfterMinutes) {
            return new Decision(true, FineType.TRAINING_LATE, Money.nz(lateAmount));
        }
        return Decision.none();
    }

    public static Decision matchLate(int minutesLate, boolean justified, int lateAfterMinutes, BigDecimal amount) {
        if (justified || minutesLate <= lateAfterMinutes) {
            return Decision.none();
        }
        return new Decision(true, FineType.MATCH_LATE, Money.nz(amount));
    }

    public static Decision matchMark(AttendanceMark mark, Integer minutesLate, boolean justified,
                                     int lateAfterMinutes, BigDecimal lateAmount, BigDecimal absenceAmount) {
        if (mark == null || mark == AttendanceMark.E || mark == AttendanceMark.I || mark == AttendanceMark.N || justified) {
            return Decision.none();
        }
        if (mark == AttendanceMark.A) {
            return new Decision(true, FineType.MATCH_ABSENCE, Money.nz(absenceAmount));
        }
        if (mark == AttendanceMark.L && minutesLate != null && minutesLate > lateAfterMinutes) {
            return new Decision(true, FineType.MATCH_LATE, Money.nz(lateAmount));
        }
        return Decision.none();
    }
}
