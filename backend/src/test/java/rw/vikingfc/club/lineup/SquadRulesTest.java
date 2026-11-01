package rw.vikingfc.club.lineup;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.player.Position;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadRulesTest {

    @Test
    void recommendedQuotasMatchTheMatchdaySizes() {
        assertEquals(Map.of("GK", 2, "DEF", 5, "MID", 5, "ATT", 3), SquadRules.recommended(15));
        assertEquals(Map.of("GK", 2, "DEF", 5, "MID", 5, "ATT", 4), SquadRules.recommended(16));
        assertEquals(Map.of("GK", 2, "DEF", 6, "MID", 6, "ATT", 4), SquadRules.recommended(18));
        assertEquals(Map.of("GK", 2, "DEF", 7, "MID", 7, "ATT", 4), SquadRules.recommended(20));
        assertEquals(23, SquadRules.sum(SquadRules.recommended(23)));
        assertEquals(12, SquadRules.sum(SquadRules.recommended(12)));
    }

    @Test
    void squadAndXiRulesUseThePublishedCodes() {
        Map<String, Integer> plan = SquadRules.recommended(18);
        assertTrue(SquadRules.squadIssues(18, plan, Map.of("GK", 2, "DEF", 6, "MID", 5, "ATT", 4), 9).contains("SQUAD_SIZE_INVALID"));
        assertTrue(SquadRules.squadIssues(18, plan, Map.of("GK", 1, "DEF", 6, "MID", 7, "ATT", 4), 9).contains("MIN_GOALKEEPERS"));
        assertTrue(SquadRules.squadIssues(18, plan, Map.of("GK", 2, "DEF", 6, "MID", 6, "ATT", 4), 9).isEmpty());
        assertTrue(SquadRules.planIssues(18, Map.of("GK", 2, "DEF", 6, "MID", 6, "ATT", 3)).contains("SQUAD_QUOTA_VIOLATION"));
        assertTrue(SquadRules.sizeIssues(10, 12, 23).contains("SQUAD_SIZE_INVALID"));
        assertTrue(SquadRules.xiIssues(10, true, false, true, false).contains("XI_NOT_11"));
        assertTrue(SquadRules.xiIssues(11, false, false, true, false).contains("MIN_GOALKEEPERS"));
        assertTrue(SquadRules.xiIssues(11, true, false, false, false).contains("PLAYER_NOT_CALLED_UP"));
        assertTrue(SquadRules.xiIssues(11, true, false, true, true).contains("PLAYER_UNAVAILABLE"));
    }

    @Test
    void positionMapKeepsAGoalkeeperInGoalAndAllowsAWingerUpFront() {
        assertTrue(SquadRules.fits("GK", Position.GOALKEEPER));
        assertFalse(SquadRules.fits("GK", Position.FORWARD));
        assertTrue(SquadRules.fits("LB", Position.LEFT_BACK));
        assertTrue(SquadRules.fits("ST", Position.FORWARD));
        assertTrue(SquadRules.fits("LW", Position.WINGER));
        assertFalse(SquadRules.fits("CB", Position.FORWARD));
        assertEquals("GK", SquadRules.lineOf("Goalkeepers", Position.GOALKEEPER));
        assertEquals("DEF", SquadRules.lineOf("Defenders", Position.CENTER_BACK));
        assertEquals("MID", SquadRules.lineOf("Midfielders", Position.CENTRAL_MIDFIELDER));
        assertEquals("ATT", SquadRules.lineOf("Attackers", Position.FORWARD));
    }

    @Test
    void participationUsesMinutesEventsAndThePlannedXi() {
        assertTrue(MatchdayLists.played(true, 0, false, false, false));
        assertFalse(MatchdayLists.played(false, 0, false, false, false));
        assertTrue(MatchdayLists.played(false, 12, false, true, true));
        assertTrue(MatchdayLists.played(false, 0, true, true, true));
        assertFalse(MatchdayLists.played(false, 0, false, true, true));
        assertTrue(MatchdayLists.played(true, 0, false, true, false));
    }

    @Test
    void illegalTransitionsKeepTheirCode() {
        BusinessException blocked = assertThrows(BusinessException.class,
                () -> LineupMachine.apply(LineupState.DRAFT, "approve", true));
        assertEquals("INVALID_TRANSITION", blocked.getCode());
    }
}
