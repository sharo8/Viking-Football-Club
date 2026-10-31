package rw.vikingfc.club.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 0;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal nz(BigDecimal value) {
        if (value == null) {
            return zero();
        }
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal maxZero(BigDecimal value) {
        BigDecimal scaled = nz(value);
        return scaled.signum() < 0 ? zero() : scaled;
    }

    public static boolean positive(BigDecimal value) {
        return nz(value).signum() > 0;
    }
}
