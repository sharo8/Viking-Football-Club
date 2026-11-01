package rw.vikingfc.club.lineup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormationBook {

    public record Spot(String code, int x, int y, String line) {
    }

    private record Named(String code, String line) {
    }

    private static final Map<String, List<List<Named>>> FORMATIONS = new LinkedHashMap<>();

    static {
        put("4-3-3", List.of(
                row(def("LB"), def("LCB"), def("RCB"), def("RB")),
                row(mid("LCM"), mid("CM"), mid("RCM")),
                row(att("LW"), att("ST"), att("RW"))));
        put("4-4-2", List.of(
                row(def("LB"), def("LCB"), def("RCB"), def("RB")),
                row(mid("LM"), mid("LCM"), mid("RCM"), mid("RM")),
                row(att("LS"), att("RS"))));
        put("4-2-3-1", List.of(
                row(def("LB"), def("LCB"), def("RCB"), def("RB")),
                row(mid("LDM"), mid("RDM")),
                row(att("LW"), mid("CAM"), att("RW")),
                row(att("ST"))));
        put("3-5-2", List.of(
                row(def("LCB"), def("CB"), def("RCB")),
                row(mid("LWB"), mid("LCM"), mid("CM"), mid("RCM"), mid("RWB")),
                row(att("LS"), att("RS"))));
        put("3-4-3", List.of(
                row(def("LCB"), def("CB"), def("RCB")),
                row(mid("LM"), mid("LCM"), mid("RCM"), mid("RM")),
                row(att("LW"), att("ST"), att("RW"))));
        put("5-3-2", List.of(
                row(mid("LWB"), def("LCB"), def("CB"), def("RCB"), mid("RWB")),
                row(mid("LCM"), mid("CM"), mid("RCM")),
                row(att("LS"), att("RS"))));
        put("4-1-4-1", List.of(
                row(def("LB"), def("LCB"), def("RCB"), def("RB")),
                row(mid("CDM")),
                row(mid("LM"), mid("LCM"), mid("RCM"), mid("RM")),
                row(att("ST"))));
        put("4-4-1-1", List.of(
                row(def("LB"), def("LCB"), def("RCB"), def("RB")),
                row(mid("LM"), mid("LCM"), mid("RCM"), mid("RM")),
                row(att("SS")),
                row(att("ST"))));
        put("5-4-1", List.of(
                row(mid("LWB"), def("LCB"), def("CB"), def("RCB"), mid("RWB")),
                row(mid("LM"), mid("LCM"), mid("RCM"), mid("RM")),
                row(att("ST"))));
    }

    private FormationBook() {
    }

    public static List<String> codes() {
        return List.copyOf(FORMATIONS.keySet());
    }

    public static List<Spot> spots(String code) {
        List<List<Named>> rows = FORMATIONS.getOrDefault(code == null ? "" : code, FORMATIONS.get("4-3-3"));
        int total = rows.size() + 1;
        List<Spot> spots = new ArrayList<>();
        spots.add(new Spot("GK", 50, SlotLayout.y(0, total), "GK"));
        for (int index = 0; index < rows.size(); index++) {
            List<Named> row = rows.get(index);
            int[] xs = SlotLayout.xs(row.size());
            int y = SlotLayout.y(index + 1, total);
            for (int place = 0; place < row.size(); place++) {
                Named named = row.get(place);
                spots.add(new Spot(named.code, xs[place], y, named.line));
            }
        }
        return List.copyOf(spots);
    }

    public static List<Integer> rowSizes(String code) {
        List<List<Named>> rows = FORMATIONS.getOrDefault(code == null ? "" : code, FORMATIONS.get("4-3-3"));
        List<Integer> sizes = new ArrayList<>();
        sizes.add(1);
        rows.forEach(row -> sizes.add(row.size()));
        return List.copyOf(sizes);
    }

    public static boolean known(String code) {
        return FORMATIONS.containsKey(code);
    }

    private static void put(String code, List<List<Named>> rows) {
        FORMATIONS.put(code, rows);
    }

    @SafeVarargs
    private static List<Named> row(Named... names) {
        return List.of(names);
    }

    private static Named def(String code) {
        return new Named(code, "DEF");
    }

    private static Named mid(String code) {
        return new Named(code, "MID");
    }

    private static Named att(String code) {
        return new Named(code, "ATT");
    }
}
