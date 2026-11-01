package rw.vikingfc.club.staff;

import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.removal.RemovalRules;
import rw.vikingfc.club.user.RolePolicy;

import java.util.regex.Pattern;

/** Checks for a staff login account. Each failure has its own error code. */
public final class StaffAccountRules {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private StaffAccountRules() {
    }

    public static void staffCategory(String code) {
        if (!"STAFF".equals(RolePolicy.category(code))) {
            throw BusinessException.badRequest("Staff accounts use a staff role", "ROLE_CATEGORY_MISMATCH");
        }
    }

    public static void email(String value) {
        if (value == null || !EMAIL.matcher(value.trim()).matches()) {
            throw BusinessException.badRequest("Enter a valid email address", "EMAIL_INVALID");
        }
    }

    public static void unique(boolean taken) {
        if (taken) {
            throw BusinessException.conflict("A user with this email already exists", "EMAIL_TAKEN", null);
        }
    }

    public static void notArchived(boolean active) {
        if (!active) {
            throw BusinessException.badRequest("This staff member is archived", "ARCHIVED");
        }
    }

    public static void headCoachSeat(String code, long activeHolders, boolean alreadyThisCoach) {
        if ("HEAD_COACH".equals(code) && !alreadyThisCoach && activeHolders >= 1) {
            throw BusinessException.conflict("That role has no seats left", "ROLE_SEATS_FULL", null);
        }
    }

    public static void reason(String code, String text) {
        RemovalRules.reason(code, text);
    }

    public static String status(boolean linked, boolean active, boolean mustChangePassword, boolean loggedIn) {
        if (!linked) {
            return "NONE";
        }
        if (!active) {
            return "SUSPENDED";
        }
        if (loggedIn) {
            return "ACTIVE";
        }
        if (mustChangePassword || !loggedIn) {
            return "INVITED";
        }
        return "ACTIVE";
    }
}
