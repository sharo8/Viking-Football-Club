package rw.vikingfc.club.attendance;

import rw.vikingfc.club.player.PlayerStatus;

import java.util.Locale;

public final class AttendanceRoster {

    private AttendanceRoster() {
    }

    public static boolean trackPlayer(PlayerStatus status, boolean active) {
        return active && status == PlayerStatus.ACTIVE;
    }

    public static boolean trackStaff(String listedIn, String department, String roleName, boolean active) {
        if (!active) {
            return false;
        }
        if (listedIn != null && "ADMINISTRATION".equalsIgnoreCase(listedIn)) {
            return false;
        }
        if (department != null && "MEDICAL".equalsIgnoreCase(department)) {
            return false;
        }
        return roleName == null || !roleName.toLowerCase(Locale.ROOT).contains("doctor");
    }
}
