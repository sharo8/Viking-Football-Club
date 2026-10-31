package rw.vikingfc.club.sanction;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.attendance.AttendanceMark;
import rw.vikingfc.club.common.util.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FineGenerationTest {

    @Test
    void thirteenAbsencesAre52000AndExcusedOrShortLateAreFree() {
        FinePolicy.Decision absence = FinePolicy.training(AttendanceMark.A, 0, false, 15, Money.of(2000), Money.of(4000));
        assertTrue(absence.fine());
        assertEquals(Money.of(4000), absence.amount());
        assertEquals(Money.of(52000), absence.amount().multiply(BigDecimals(13)));

        assertFalse(FinePolicy.training(AttendanceMark.E, 0, true, 15, Money.of(2000), Money.of(4000)).fine());
        assertFalse(FinePolicy.training(AttendanceMark.A, 0, true, 15, Money.of(2000), Money.of(4000)).fine());
        assertFalse(FinePolicy.training(AttendanceMark.L, 15, false, 15, Money.of(2000), Money.of(4000)).fine());
        FinePolicy.Decision late = FinePolicy.training(AttendanceMark.L, 16, false, 15, Money.of(2000), Money.of(4000));
        assertTrue(late.fine());
        assertEquals(FineType.TRAINING_LATE, late.type());
        assertEquals(Money.of(2000), late.amount());
        assertFalse(FinePolicy.matchLate(10, false, 15, Money.of(2000)).fine());
        assertTrue(FinePolicy.matchLate(20, false, 15, Money.of(2000)).fine());
    }

    private static java.math.BigDecimal BigDecimals(int count) {
        return java.math.BigDecimal.valueOf(count);
    }
}
