package rw.vikingfc.club.lineup;

import rw.vikingfc.club.common.exception.BusinessException;

public final class LineupMachine {

    private LineupMachine() {
    }

    public static LineupState apply(LineupState from, String action, boolean executive) {
        LineupState state = from == null ? LineupState.DRAFT : from;
        LineupState next = switch (action) {
            case "submit" -> state == LineupState.DRAFT || state == LineupState.CHANGES_REQUESTED ? LineupState.SUBMITTED : null;
            case "approve" -> executive && state == LineupState.SUBMITTED ? LineupState.APPROVED : null;
            case "changes" -> executive && state == LineupState.SUBMITTED ? LineupState.CHANGES_REQUESTED : null;
            case "reject" -> executive && (state == LineupState.SUBMITTED || state == LineupState.APPROVED) ? LineupState.DRAFT : null;
            case "publish" -> state == LineupState.APPROVED || (executive && state != LineupState.LOCKED && state != LineupState.FINISHED)
                    ? LineupState.PUBLISHED : null;
            case "unpublish" -> state == LineupState.PUBLISHED ? LineupState.DRAFT : null;
            case "lock" -> state == LineupState.PUBLISHED ? LineupState.LOCKED : null;
            case "unlock" -> executive && state == LineupState.LOCKED ? LineupState.PUBLISHED : null;
            case "finish" -> state == LineupState.LOCKED || state == LineupState.PUBLISHED ? LineupState.FINISHED : null;
            default -> null;
        };
        if (next == null) {
            throw BusinessException.unprocessable("This lineup cannot move from " + state + " by " + action, "INVALID_TRANSITION");
        }
        return next;
    }

    public static String reminder(java.time.Instant kickoff, java.time.Instant now, boolean submitted, int hours) {
        if (kickoff == null || now == null || submitted) {
            return null;
        }
        java.time.Instant deadline = kickoff.minusSeconds(hours * 3600L);
        if (!now.isBefore(deadline)) {
            return "OVERDUE";
        }
        if (!now.isBefore(kickoff.minusSeconds(50 * 3600L))) {
            return "T50";
        }
        if (!now.isBefore(kickoff.minusSeconds(72 * 3600L))) {
            return "T72";
        }
        return null;
    }
}
