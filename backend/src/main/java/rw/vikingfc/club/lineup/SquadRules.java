package rw.vikingfc.club.lineup;

import rw.vikingfc.club.player.Position;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SquadRules {

    public static final List<String> LINES = List.of("GK", "DEF", "MID", "ATT");

    private SquadRules() {
    }

    public static Map<String, Integer> recommended(int size) {
        int safe = Math.max(size, 11);
        int gk = safe >= 12 ? 2 : 1;
        int rest = safe - gk;
        int def = (int) Math.round(rest * 6.0 / 16.0);
        int mid = (int) Math.round(rest * 6.0 / 16.0);
        int att = rest - def - mid;
        if (att < 1 && rest >= 3) {
            if (def >= mid && def > 1) {
                def--;
            } else if (mid > 1) {
                mid--;
            }
            att++;
        }
        Map<String, Integer> plan = new LinkedHashMap<>();
        plan.put("GK", gk);
        plan.put("DEF", Math.max(def, 0));
        plan.put("MID", Math.max(mid, 0));
        plan.put("ATT", Math.max(att, 0));
        return plan;
    }

    public static String sizeGate(int size, int min, int max, int competitionMax, boolean executive, String reason) {
        if (size < Math.max(11, min) || size > max) {
            return "SQUAD_SIZE_OUT_OF_RANGE";
        }
        boolean allowed = executive && reason != null && !reason.isBlank();
        if (size > competitionMax && !allowed) {
            return "SQUAD_SIZE_OUT_OF_RANGE";
        }
        return null;
    }

    public static int sum(Map<String, Integer> plan) {
        if (plan == null) {
            return 0;
        }
        return LINES.stream().mapToInt(line -> Math.max(0, plan.getOrDefault(line, 0))).sum();
    }

    public static String lineOf(String lineName, Position position) {
        if (position == Position.GOALKEEPER) {
            return "GK";
        }
        String name = lineName == null ? "" : lineName.toLowerCase(Locale.ROOT);
        if (name.startsWith("goal")) {
            return "GK";
        }
        if (name.startsWith("def")) {
            return "DEF";
        }
        if (name.startsWith("mid")) {
            return "MID";
        }
        if (name.startsWith("att")) {
            return "ATT";
        }
        return switch (position) {
            case CENTER_BACK, LEFT_BACK, RIGHT_BACK -> "DEF";
            case DEFENSIVE_MIDFIELDER, CENTRAL_MIDFIELDER, OFFENSIVE_MIDFIELDER -> "MID";
            case WINGER, FORWARD -> "ATT";
            case GOALKEEPER -> "GK";
        };
    }

    public static boolean fits(String slotCode, Position position) {
        if (slotCode == null || position == null) {
            return false;
        }
        String slot = slotCode.toUpperCase(Locale.ROOT);
        return switch (slot) {
            case "GK" -> position == Position.GOALKEEPER;
            case "LB", "LWB" -> position == Position.LEFT_BACK || position == Position.WINGER;
            case "RB", "RWB" -> position == Position.RIGHT_BACK || position == Position.WINGER;
            case "CB", "LCB", "RCB" -> position == Position.CENTER_BACK;
            case "CDM", "DM", "LDM", "RDM" -> position == Position.DEFENSIVE_MIDFIELDER || position == Position.CENTRAL_MIDFIELDER;
            case "CM", "LCM", "RCM" -> position == Position.CENTRAL_MIDFIELDER
                    || position == Position.DEFENSIVE_MIDFIELDER
                    || position == Position.OFFENSIVE_MIDFIELDER;
            case "CAM", "AM", "SS" -> position == Position.OFFENSIVE_MIDFIELDER || position == Position.FORWARD;
            case "LM", "RM", "LW", "RW" -> position == Position.WINGER
                    || position == Position.OFFENSIVE_MIDFIELDER
                    || position == Position.FORWARD;
            case "ST", "CF", "LS", "RS" -> position == Position.FORWARD || position == Position.WINGER;
            default -> false;
        };
    }

    public static List<String> sizeIssues(int size, int minSquad, int maxSquad) {
        List<String> issues = new ArrayList<>();
        if (size < Math.max(11, minSquad) || size > maxSquad) {
            issues.add("SQUAD_SIZE_INVALID");
        }
        return issues;
    }

    public static List<String> planIssues(int size, Map<String, Integer> plan) {
        List<String> issues = new ArrayList<>();
        if (plan == null || sum(plan) != size) {
            issues.add("SQUAD_QUOTA_VIOLATION");
        }
        if (plan != null && plan.getOrDefault("GK", 0) < 2 && size >= 12) {
            issues.add("MIN_GOALKEEPERS");
        }
        return issues;
    }

    public static List<String> squadIssues(int size, Map<String, Integer> plan, Map<String, Integer> selected, int maxBench) {
        List<String> issues = new ArrayList<>();
        int total = sum(selected);
        if (total != size) {
            issues.add("SQUAD_SIZE_INVALID");
        }
        if (plan != null) {
            for (String line : LINES) {
                if (selected.getOrDefault(line, 0) != plan.getOrDefault(line, 0)) {
                    issues.add("SQUAD_QUOTA_VIOLATION");
                    break;
                }
            }
        }
        if (selected.getOrDefault("GK", 0) < 2 && size >= 12) {
            issues.add("MIN_GOALKEEPERS");
        }
        int bench = size - 11;
        if (bench < 0 || bench > maxBench) {
            issues.add("SQUAD_SIZE_INVALID");
        }
        return issues.stream().distinct().toList();
    }

    public static List<String> xiIssues(int starterCount, boolean goalkeeperInGoal, boolean duplicates, boolean onlyCalled, boolean unavailable) {
        List<String> issues = new ArrayList<>();
        if (starterCount != 11) {
            issues.add("XI_NOT_11");
        }
        if (!goalkeeperInGoal) {
            issues.add("MIN_GOALKEEPERS");
        }
        if (duplicates) {
            issues.add("XI_NOT_11");
        }
        if (!onlyCalled) {
            issues.add("PLAYER_NOT_CALLED_UP");
        }
        if (unavailable) {
            issues.add("PLAYER_UNAVAILABLE");
        }
        return issues.stream().distinct().toList();
    }
}
