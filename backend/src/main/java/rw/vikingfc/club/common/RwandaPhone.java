package rw.vikingfc.club.common;

import rw.vikingfc.club.common.exception.BusinessException;

public final class RwandaPhone {
    private RwandaPhone() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return "";
        }
        if (digits.startsWith("250")) {
            digits = digits.substring(3);
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return "+250" + digits;
    }

    public static boolean valid(String normalized) {
        return normalized != null && normalized.matches("^\\+2507[2389][0-9]{7}$");
    }

    public static void require(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        if (!valid(normalize(raw))) {
            throw BusinessException.badRequest(
                    "Use a Rwanda mobile number: +250 and 9 digits starting with 72, 73, 78 or 79.",
                    "PHONE_INVALID");
        }
    }

    public static String store(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        require(raw);
        return normalize(raw);
    }
}
