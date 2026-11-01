package rw.vikingfc.club.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StandingsMath {

    private StandingsMath() {
    }

    public static final class Row {
        public final String name;
        public final boolean club;
        public int played;
        public int won;
        public int drawn;
        public int lost;
        public int goalsFor;
        public int goalsAgainst;
        private final StringBuilder form = new StringBuilder();

        public Row(String name, boolean club) {
            this.name = name;
            this.club = club;
        }

        public int points(int win, int draw, int loss) {
            return won * win + drawn * draw + lost * loss;
        }

        public int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        public String formText() {
            String text = form.toString();
            return text.length() <= 5 ? text : text.substring(text.length() - 5);
        }
    }

    public static void apply(Row us, Row them, int goalsFor, int goalsAgainst) {
        us.played++;
        them.played++;
        us.goalsFor += goalsFor;
        us.goalsAgainst += goalsAgainst;
        them.goalsFor += goalsAgainst;
        them.goalsAgainst += goalsFor;
        if (goalsFor > goalsAgainst) {
            us.won++;
            them.lost++;
            us.form.append('W');
            them.form.append('L');
        } else if (goalsFor < goalsAgainst) {
            us.lost++;
            them.won++;
            us.form.append('L');
            them.form.append('W');
        } else {
            us.drawn++;
            them.drawn++;
            us.form.append('D');
            them.form.append('D');
        }
    }

    public static Map<String, Row> table() {
        return new LinkedHashMap<>();
    }

    public static Row row(Map<String, Row> table, String name, boolean club) {
        return table.computeIfAbsent(name, key -> new Row(key, club));
    }

    public static List<Row> rank(Collection<Row> rows, int win, int draw, int loss) {
        List<Row> ordered = new ArrayList<>(rows);
        ordered.sort(Comparator.comparingInt((Row row) -> row.points(win, draw, loss)).reversed()
                .thenComparing(Comparator.comparingInt(Row::goalDifference).reversed())
                .thenComparing(Comparator.comparingInt((Row row) -> row.goalsFor).reversed())
                .thenComparing(row -> row.name, String.CASE_INSENSITIVE_ORDER));
        return ordered;
    }
}
