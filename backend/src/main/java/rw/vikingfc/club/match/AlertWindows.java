package rw.vikingfc.club.match;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AlertWindows {

    private static final int[] MARKS = {72, 48, 36, 24, 12, 6, 3, 2};

    private AlertWindows() {
    }

    public static List<String> due(Instant kickoff, Instant now, boolean stopped) {
        if (stopped || kickoff == null || !now.isBefore(kickoff)) {
            return List.of();
        }
        long hours = Duration.between(now, kickoff).toHours();
        if (hours > 72) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (int mark : MARKS) {
            if (hours <= mark) {
                keys.add("T-" + mark);
            }
        }
        return keys;
    }

    public static boolean email(String window, boolean overdue) {
        return "T-72".equals(window) || "T-48".equals(window) || "T-24".equals(window) || (overdue && "T-36".equals(window));
    }
}
