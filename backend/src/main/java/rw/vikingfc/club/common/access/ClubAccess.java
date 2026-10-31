package rw.vikingfc.club.common.access;

import rw.vikingfc.club.common.util.SecurityUtils;

public final class ClubAccess {

    private ClubAccess() {
    }

    public enum PlayerView { PUBLIC, OPERATIONAL, FULL }

    public static PlayerView playerView() {
        if (SecurityUtils.isExecutive()) {
            return PlayerView.FULL;
        }
        if (SecurityUtils.hasRole("SOCIAL_MEDIA_MANAGER") || SecurityUtils.hasRole("MARKETING_MANAGER")) {
            return PlayerView.PUBLIC;
        }
        return PlayerView.OPERATIONAL;
    }

    public static boolean goalkeepersOnly() {
        return SecurityUtils.hasRole("GOALKEEPER_COACH") && !SecurityUtils.isExecutive();
    }

    public static boolean medicalDetail() {
        return SecurityUtils.isExecutive() || SecurityUtils.hasRole("DOCTOR");
    }

    public static boolean coachMedical() {
        return SecurityUtils.hasRole("HEAD_COACH")
                || SecurityUtils.hasRole("ASSISTANT_COACH")
                || SecurityUtils.hasRole("GOALKEEPER_COACH");
    }

    public static boolean publishedOnly() {
        return !SecurityUtils.isExecutive()
                && (SecurityUtils.hasRole("SOCIAL_MEDIA_MANAGER") || SecurityUtils.hasRole("MARKETING_MANAGER"));
    }
}
