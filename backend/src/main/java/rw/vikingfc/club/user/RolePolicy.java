package rw.vikingfc.club.user;

import java.util.Locale;
import java.util.Set;

/**
 * Category and OTP defaults for the seeded roles. Custom roles live in club_roles
 * and follow the flags stored there.
 */
public final class RolePolicy {

    public static final Set<String> OTP_ON = Set.of(
            "PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER", "HEAD_COACH", "DOCTOR", "KIT_MANAGER");

    public static final Set<String> EXECUTIVE = Set.of("PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER");

    private RolePolicy() {
    }

    public static String codeOf(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        String raw = label.trim().toUpperCase(Locale.ROOT).replace('-', ' ');
        if (raw.contains("FINANCE")) return "FINANCE_MANAGER";
        if (raw.contains("PRESIDENT")) return "PRESIDENT";
        if (raw.contains("GENERAL")) return "GENERAL_MANAGER";
        if (raw.contains("SOCIAL")) return "SOCIAL_MEDIA_MANAGER";
        if (raw.contains("MARKETING")) return "MARKETING_MANAGER";
        if (raw.contains("ASSISTANT MANAGER")) return "ASSISTANT_MANAGER";
        if (raw.contains("HEAD COACH")) return "HEAD_COACH";
        if (raw.contains("GOALKEEPER")) return "GOALKEEPER_COACH";
        if (raw.contains("ASSISTANT COACH")) return "ASSISTANT_COACH";
        if (raw.contains("FITNESS")) return "FITNESS_COACH";
        if (raw.contains("PHYSIO")) return "PHYSIOTHERAPIST";
        if (raw.contains("KIT")) return "KIT_MANAGER";
        if (raw.contains("TEAM MANAGER")) return "TEAM_MANAGER";
        if (raw.contains("DOCTOR")) return "DOCTOR";
        if (raw.equals("PLAYER")) return "PLAYER";
        return raw.replace(' ', '_');
    }

    public static String category(String code) {
        return switch (code == null ? "" : code) {
            case "PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER", "ASSISTANT_MANAGER", "SOCIAL_MEDIA_MANAGER", "MARKETING_MANAGER" -> "ADMINISTRATION";
            case "PLAYER" -> "PLAYER";
            case "HEAD_COACH", "ASSISTANT_COACH", "GOALKEEPER_COACH", "FITNESS_COACH", "DOCTOR", "PHYSIOTHERAPIST", "KIT_MANAGER", "TEAM_MANAGER" -> "STAFF";
            default -> "STAFF";
        };
    }

    public static boolean otpRequired(String code) {
        return code != null && OTP_ON.contains(code);
    }

    public static boolean executive(String code) {
        return code != null && EXECUTIVE.contains(code);
    }

    /** These four cannot be renamed or deleted. Every other role can. */
    public static boolean locked(String code) {
        return "PRESIDENT".equals(code) || "GENERAL_MANAGER".equals(code) || "FINANCE_MANAGER".equals(code) || "PLAYER".equals(code);
    }

    public static Integer seats(String code) {
        if ("PRESIDENT".equals(code)) {
            return 1;
        }
        if ("HEAD_COACH".equals(code)) {
            return 1;
        }
        return null;
    }
}
