package rw.vikingfc.club.player;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BirthdayServiceTest {

    @Test
    void leapDayIsCelebratedOn28FebruaryWhenTheYearIsNotALeapYear() {
        LocalDate birth = LocalDate.of(2000, 2, 29);
        assertEquals(LocalDate.of(2026, 2, 28), BirthdayService.celebration(birth, 2026));
        assertEquals(LocalDate.of(2028, 2, 29), BirthdayService.celebration(birth, 2028));
    }
}
