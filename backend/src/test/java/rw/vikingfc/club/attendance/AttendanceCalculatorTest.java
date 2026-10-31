package rw.vikingfc.club.attendance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttendanceCalculatorTest {

    @Test
    void dayOneAndDayTwoMatchTheWorkbook() {
        int day1Present = 13;
        int day1Permission = 4;
        int day1Absent = 8;
        int day2Present = 13;
        int day2Permission = 7;
        int day2Absent = 5;
        assertEquals(25, day1Present + day1Permission + day1Absent);
        assertEquals(25, day2Present + day2Permission + day2Absent);
        BigDecimal squad = AttendanceCalculator.percentage(
                day1Present + day2Present, 0, day1Permission + day2Permission, day1Absent + day2Absent);
        assertEquals(new BigDecimal("52.0"), squad);
        assertEquals("At risk", AttendanceCalculator.band(new BigDecimal("50.0"), 80, 60));
        assertEquals("At risk", AttendanceCalculator.band(new BigDecimal("0.0"), 80, 60));
        assertEquals("On track", AttendanceCalculator.band(new BigDecimal("100.0"), 80, 60));
        assertEquals("Watch", AttendanceCalculator.band(new BigDecimal("60.0"), 80, 60));
    }
}
