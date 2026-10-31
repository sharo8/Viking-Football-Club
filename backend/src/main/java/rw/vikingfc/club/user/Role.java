package rw.vikingfc.club.user;

public enum Role {
    PRESIDENT,
    GENERAL_MANAGER,
    FINANCE_MANAGER,
    ASSISTANT_MANAGER,
    DOCTOR,
    SOCIAL_MEDIA_MANAGER,
    MARKETING_MANAGER,
    HEAD_COACH,
    ASSISTANT_COACH,
    GOALKEEPER_COACH;

    public boolean isExecutive() {
        return this == PRESIDENT || this == GENERAL_MANAGER || this == FINANCE_MANAGER;
    }
}
