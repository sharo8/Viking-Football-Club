package rw.vikingfc.club.lineup;

public final class MatchdayLists {

    private MatchdayLists() {
    }

    public static boolean played(boolean starter, int minutes, boolean entered, boolean liveOrFinished, boolean minutesKnown) {
        if (!liveOrFinished) {
            return starter;
        }
        if (minutes > 0 || entered) {
            return true;
        }
        return starter && !minutesKnown;
    }
}
