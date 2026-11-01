package rw.vikingfc.club.removal;

import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.user.RolePolicy;

import java.util.Locale;
import java.util.Set;

/** Checks for role removal and account removal. Each failure has its own error code. */
public final class RemovalRules {

    public static final Set<String> REASONS = Set.of(
            "RESIGNED", "CONTRACT_ENDED", "MISCONDUCT", "DUPLICATE", "MISTAKE", "OTHER");
    public static final Set<String> EXIT_REASONS = Set.of(
            "TRANSFERRED", "RELEASED", "RETIRED", "CONTRACT_ENDED", "LOAN_ENDED", "OTHER");

    private RemovalRules() {
    }

    public static void roleMayBeDeleted(String code) {
        if (RolePolicy.locked(code)) {
            throw BusinessException.badRequest("This role is protected", "ROLE_LOCKED");
        }
    }

    public static void sameCategory(String from, String to) {
        if (from == null || to == null || !from.equalsIgnoreCase(to)) {
            throw BusinessException.badRequest("That role is in another category", "ROLE_CATEGORY_MISMATCH");
        }
    }

    public static void notSelf(Long actorId, Long userId) {
        if (actorId != null && actorId.equals(userId)) {
            throw BusinessException.badRequest("You cannot change your own account", "SELF");
        }
    }

    public static void seats(Integer max, long already, int incoming) {
        if (max != null && already + incoming > max) {
            throw BusinessException.conflict("That role has no seats left", "ROLE_SEATS_FULL", null);
        }
    }

    public static void notLastExecutive(String roleCode, long activeWithThatRole) {
        if (RolePolicy.executive(roleCode) && activeWithThatRole <= 1) {
            throw BusinessException.badRequest("The last active executive cannot be removed", "LAST_EXECUTIVE");
        }
    }

    public static void reason(String code, String text) {
        String kind = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String note = text == null ? "" : text.trim();
        if (!REASONS.contains(kind) || note.length() > 500) {
            throw BusinessException.badRequest("A reason is required", "REASON_REQUIRED");
        }
        if ("OTHER".equals(kind) && note.length() < 10) {
            throw BusinessException.badRequest("A reason is required", "REASON_REQUIRED");
        }
        if (!"OTHER".equals(kind) && !note.isEmpty() && note.length() < 10) {
            throw BusinessException.badRequest("A reason is required", "REASON_REQUIRED");
        }
    }

    public static void exitReason(String code) {
        String kind = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (!EXIT_REASONS.contains(kind)) {
            throw BusinessException.badRequest("Choose why the player is leaving", "EXIT_REASON");
        }
    }

    public static void deletionReason(String text) {
        String note = text == null ? "" : text.trim();
        if (note.length() < 20 || note.length() > 2000) {
            throw BusinessException.badRequest("The reason must be at least 20 characters", "REASON_REQUIRED");
        }
    }

    public static void confirmName(String expected, String typed) {
        String left = expected == null ? "" : expected.trim();
        String right = typed == null ? "" : typed.trim();
        if (left.isEmpty() || !left.equalsIgnoreCase(right)) {
            throw BusinessException.badRequest("Type the person's name to confirm", "CONFIRM_NAME");
        }
    }

    public static String afterVotes(boolean rejected, int approvals) {
        if (rejected) {
            return "REJECTED";
        }
        if (approvals >= 3) {
            return "APPROVED";
        }
        return "PENDING";
    }

    public static void openRequest(boolean alreadyOpen) {
        if (alreadyOpen) {
            throw BusinessException.badRequest("This player already has an open request", "REQUEST_OPEN");
        }
    }

    public static void formerOnly(boolean former) {
        if (!former) {
            throw BusinessException.badRequest("Only a former player can be deleted this way", "NOT_FORMER");
        }
    }

    public static void executivesPresent(long presidents, long managers, long finance) {
        if (presidents < 1 || managers < 1 || finance < 1) {
            throw BusinessException.badRequest("The club needs an active President, General Manager and Finance Manager", "EXECUTIVES_MISSING");
        }
    }

    public static void oneVote(boolean alreadyVoted) {
        if (alreadyVoted) {
            throw BusinessException.badRequest("You have already voted", "VOTE_LOCKED");
        }
    }
}
