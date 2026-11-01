package rw.vikingfc.club.lineup;

public final class SubRules {

    private SubRules() {
    }

    public static String check(int minute, int used, int max, boolean alreadyReplaced, boolean outKeeper, boolean inKeeper, boolean override) {
        if (minute < 1 || minute > 120) {
            return "INVALID_MINUTE";
        }
        if (used >= max) {
            return "SUB_LIMIT_REACHED";
        }
        if (alreadyReplaced) {
            return "PLAYER_ALREADY_REPLACED";
        }
        if (outKeeper && !inKeeper && !override) {
            return "GK_REPLACEMENT_INVALID";
        }
        return null;
    }
}
