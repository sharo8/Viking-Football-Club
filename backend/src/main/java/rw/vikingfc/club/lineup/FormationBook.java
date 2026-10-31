package rw.vikingfc.club.lineup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormationBook {

    public record Spot(String code, int x, int y, String line) {
    }

    private static final Map<String, List<Spot>> FORMATIONS = new LinkedHashMap<>();

    static {
        put("4-3-3",
                gk(), def("LB", 14, 68), def("LCB", 36, 72), def("RCB", 64, 72), def("RB", 86, 68),
                mid("LCM", 28, 48), mid("CM", 50, 50), mid("RCM", 72, 48),
                att("LW", 18, 22), att("ST", 50, 14), att("RW", 82, 22));
        put("4-4-2",
                gk(), def("LB", 14, 68), def("LCB", 36, 72), def("RCB", 64, 72), def("RB", 86, 68),
                mid("LM", 16, 46), mid("LCM", 38, 50), mid("RCM", 62, 50), mid("RM", 84, 46),
                att("LS", 38, 16), att("RS", 62, 16));
        put("4-2-3-1",
                gk(), def("LB", 14, 70), def("LCB", 36, 74), def("RCB", 64, 74), def("RB", 86, 70),
                mid("LDM", 38, 56), mid("RDM", 62, 56),
                att("LW", 18, 34), mid("CAM", 50, 36), att("RW", 82, 34),
                att("ST", 50, 14));
        put("3-5-2",
                gk(), def("LCB", 28, 72), def("CB", 50, 74), def("RCB", 72, 72),
                mid("LWB", 12, 48), mid("LCM", 32, 52), mid("CM", 50, 54), mid("RCM", 68, 52), mid("RWB", 88, 48),
                att("LS", 38, 16), att("RS", 62, 16));
        put("3-4-3",
                gk(), def("LCB", 28, 72), def("CB", 50, 74), def("RCB", 72, 72),
                mid("LM", 16, 48), mid("LCM", 38, 52), mid("RCM", 62, 52), mid("RM", 84, 48),
                att("LW", 18, 20), att("ST", 50, 14), att("RW", 82, 20));
        put("5-3-2",
                gk(), mid("LWB", 10, 64), def("LCB", 30, 72), def("CB", 50, 74), def("RCB", 70, 72), mid("RWB", 90, 64),
                mid("LCM", 30, 48), mid("CM", 50, 50), mid("RCM", 70, 48),
                att("LS", 38, 16), att("RS", 62, 16));
        put("4-1-4-1",
                gk(), def("LB", 14, 70), def("LCB", 36, 74), def("RCB", 64, 74), def("RB", 86, 70),
                mid("CDM", 50, 58),
                mid("LM", 16, 40), mid("LCM", 38, 42), mid("RCM", 62, 42), mid("RM", 84, 40),
                att("ST", 50, 14));
        put("4-4-1-1",
                gk(), def("LB", 14, 72), def("LCB", 36, 74), def("RCB", 64, 74), def("RB", 86, 72),
                mid("LM", 16, 52), mid("LCM", 38, 54), mid("RCM", 62, 54), mid("RM", 84, 52),
                att("SS", 50, 30), att("ST", 50, 14));
        put("5-4-1",
                gk(), mid("LWB", 10, 66), def("LCB", 30, 72), def("CB", 50, 74), def("RCB", 70, 72), mid("RWB", 90, 66),
                mid("LM", 22, 46), mid("LCM", 40, 50), mid("RCM", 60, 50), mid("RM", 78, 46),
                att("ST", 50, 16));
    }

    private FormationBook() {
    }

    public static List<String> codes() {
        return List.copyOf(FORMATIONS.keySet());
    }

    public static List<Spot> spots(String code) {
        return FORMATIONS.getOrDefault(code == null ? "" : code, FORMATIONS.get("4-3-3"));
    }

    public static boolean known(String code) {
        return FORMATIONS.containsKey(code);
    }

    private static void put(String code, Spot... spots) {
        FORMATIONS.put(code, List.of(spots));
    }

    private static Spot gk() {
        return new Spot("GK", 50, 86, "GK");
    }

    private static Spot def(String code, int x, int y) {
        return new Spot(code, x, y, "DEF");
    }

    private static Spot mid(String code, int x, int y) {
        return new Spot(code, x, y, "MID");
    }

    private static Spot att(String code, int x, int y) {
        return new Spot(code, x, y, "ATT");
    }
}
