package rw.vikingfc.club.attendance;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.player.PlayerStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceRosterTest {

    @Test
    void doctorsAndFormerPlayersStayOffTheList() {
        assertFalse(AttendanceRoster.trackStaff("STAFF", "Medical", "Physiotherapist", true));
        assertFalse(AttendanceRoster.trackStaff("STAFF", "Support", "Doctor", true));
        assertFalse(AttendanceRoster.trackStaff("ADMINISTRATION", "Administration", "Team Manager", true));
        assertTrue(AttendanceRoster.trackStaff("STAFF", "Technical", "Head Coach", true));
        assertFalse(AttendanceRoster.trackPlayer(PlayerStatus.FORMER, true));
        assertFalse(AttendanceRoster.trackPlayer(PlayerStatus.LOANED, true));
        assertTrue(AttendanceRoster.trackPlayer(PlayerStatus.ACTIVE, true));
    }

    @Test
    void monthPresetKeepsTheSampleDaysAndWeekGroupsThem() {
        LocalDate today = LocalDate.of(2026, 10, 31);
        LocalDate[] month = AttendanceRange.bounds("month", null, null, today);
        assertTrue(AttendanceRange.in(LocalDate.of(2026, 10, 1), month[0], month[1]));
        assertTrue(AttendanceRange.in(LocalDate.of(2026, 10, 2), month[0], month[1]));
        assertFalse(AttendanceRange.in(LocalDate.of(2026, 9, 30), month[0], month[1]));
        assertEquals(AttendanceRange.bucket(LocalDate.of(2026, 10, 1), "week"), AttendanceRange.bucket(LocalDate.of(2026, 10, 2), "week"));
    }
}
