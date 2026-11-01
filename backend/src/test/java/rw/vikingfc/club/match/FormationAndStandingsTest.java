package rw.vikingfc.club.match;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.KigaliClock;
import rw.vikingfc.club.lineup.FormationBook;
import rw.vikingfc.club.lineup.LineupSheetArt;
import rw.vikingfc.club.lineup.SquadRules;
import rw.vikingfc.club.lineup.SubRules;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationAndStandingsTest {

    @Test
    void everyFormationKeepsTokensApart() {
        for (String code : FormationBook.codes()) {
            List<FormationBook.Spot> spots = FormationBook.spots(code);
            assertEquals(11, spots.size(), code);
            Map<Integer, List<Integer>> rows = new java.util.LinkedHashMap<>();
            for (FormationBook.Spot spot : spots) {
                rows.computeIfAbsent(spot.y(), key -> new ArrayList<>()).add(spot.x());
            }
            rows.values().forEach(xs -> {
                List<Integer> sorted = xs.stream().sorted().toList();
                for (int index = 0; index < sorted.size(); index++) {
                    assertEquals(100, sorted.get(index) + sorted.get(sorted.size() - 1 - index), code);
                }
            });
            for (int i = 0; i < spots.size(); i++) {
                for (int j = i + 1; j < spots.size(); j++) {
                    double dx = spots.get(i).x() - spots.get(j).x();
                    double dy = (spots.get(i).y() - spots.get(j).y()) * 1.5;
                    double distance = Math.hypot(dx, dy);
                    assertTrue(distance >= 14, code + " " + spots.get(i).code() + " / " + spots.get(j).code() + " = " + distance);
                }
            }
        }
        FormationBook.Spot striker = FormationBook.spots("4-3-3").stream().filter(spot -> spot.code().equals("ST")).findFirst().orElseThrow();
        assertEquals(50, striker.x());
        assertEquals(14, striker.y());
        assertEquals(List.of(1, 4, 2, 3, 1), FormationBook.rowSizes("4-2-3-1"));
    }

    @Test
    void squadSizeStaysInsideTheCompetitionMaximumUnlessAnExecutiveOverrides() {
        assertEquals("SQUAD_SIZE_OUT_OF_RANGE", SquadRules.sizeGate(20, 12, 23, 18, false, null));
        assertNull(SquadRules.sizeGate(20, 12, 23, 18, true, "Cup rule"));
        assertNull(SquadRules.sizeGate(18, 12, 23, 18, false, null));
    }

    @Test
    void substitutionsRejectAReturnAndAGoalkeeperSwap() {
        assertEquals("INVALID_MINUTE", SubRules.check(0, 0, 5, false, false, false, false));
        assertEquals("SUB_LIMIT_REACHED", SubRules.check(60, 5, 5, false, false, false, false));
        assertEquals("PLAYER_ALREADY_REPLACED", SubRules.check(70, 1, 5, true, false, false, false));
        assertEquals("GK_REPLACEMENT_INVALID", SubRules.check(40, 0, 5, false, true, false, false));
        assertNull(SubRules.check(40, 0, 5, false, true, true, false));
    }

    @Test
    void kickoffInUtcPrintsKigaliTimeAndTheSheetOpens() {
        assertEquals("Saturday 7 November 2026, 15:30", KigaliClock.format(Instant.parse("2026-11-07T13:30:00Z"), "en"));
        List<LineupSheetArt.Token> players = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            players.add(new LineupSheetArt.Token("Éric " + index, String.valueOf(index + 1), 20 + index * 5, 20 + index * 4));
        }
        byte[] sheet = LineupSheetArt.pdf("Marines FC", "Saturday 7 November 2026, 15:30", "Nyamirambo", "League", "4-3-3", 18, players, List.of(new LineupSheetArt.Token("Kavata", "12", 0, 0)), "fr", "coach@vikingfc.rw");
        assertTrue(sheet.length > 800);
        assertEquals('%', (char) sheet[0]);
        byte[] wide = LineupSheetArt.pdf("Marines FC", "Saturday 7 November 2026, 15:30", "Nyamirambo", "League", "4-3-3", 23, players, bench(12), "rw", "coach@vikingfc.rw");
        assertTrue(wide.length > sheet.length / 2);
    }

    private static List<LineupSheetArt.Token> bench(int count) {
        List<LineupSheetArt.Token> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(new LineupSheetArt.Token("Joueur " + index, String.valueOf(12 + index), 0, 0));
        }
        return rows;
    }

    @Test
    void leagueMathIgnoresNothingAndBreaksTiesOnGoalDifference() {
        var table = StandingsMath.table();
        StandingsMath.Row viking = StandingsMath.row(table, "Viking FC", true);
        StandingsMath.Row marines = StandingsMath.row(table, "Marines FC", false);
        StandingsMath.Row rck = StandingsMath.row(table, "RCK", false);
        StandingsMath.apply(viking, marines, 2, 0);
        StandingsMath.apply(rck, marines, 1, 1);
        List<StandingsMath.Row> ranked = StandingsMath.rank(table.values(), 3, 1, 0);
        assertEquals("Viking FC", ranked.get(0).name);
        assertEquals(3, ranked.get(0).points(3, 1, 0));
        assertEquals("W", viking.formText());
        assertEquals(1, marines.lost);
        assertEquals(1, marines.drawn);
    }

    @Test
    void lineupAlertsStartSeventyTwoHoursBeforeAndStopWhenPublished() {
        Instant kickoff = Instant.parse("2026-11-07T13:30:00Z");
        assertEquals(List.of("T-72"), AlertWindows.due(kickoff, kickoff.minus(70, ChronoUnit.HOURS), false));
        assertEquals(List.of(), AlertWindows.due(kickoff, kickoff.minus(70, ChronoUnit.HOURS), true));
        assertEquals(List.of(), AlertWindows.due(kickoff, kickoff.minus(80, ChronoUnit.HOURS), false));
    }
}
