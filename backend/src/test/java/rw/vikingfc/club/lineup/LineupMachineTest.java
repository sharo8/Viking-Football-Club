package rw.vikingfc.club.lineup;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineupMachineTest {

    @Test
    void everyTransitionFollowsTheStateMachine() {
        assertEquals(LineupState.SUBMITTED, LineupMachine.apply(LineupState.DRAFT, "submit", false));
        assertEquals(LineupState.SUBMITTED, LineupMachine.apply(LineupState.CHANGES_REQUESTED, "submit", false));
        assertEquals(LineupState.APPROVED, LineupMachine.apply(LineupState.SUBMITTED, "approve", true));
        assertEquals(LineupState.CHANGES_REQUESTED, LineupMachine.apply(LineupState.SUBMITTED, "changes", true));
        assertEquals(LineupState.DRAFT, LineupMachine.apply(LineupState.SUBMITTED, "reject", true));
        assertEquals(LineupState.PUBLISHED, LineupMachine.apply(LineupState.APPROVED, "publish", false));
        assertEquals(LineupState.PUBLISHED, LineupMachine.apply(LineupState.DRAFT, "publish", true));
        assertEquals(LineupState.DRAFT, LineupMachine.apply(LineupState.PUBLISHED, "unpublish", false));
        assertEquals(LineupState.LOCKED, LineupMachine.apply(LineupState.PUBLISHED, "lock", false));
        assertEquals(LineupState.PUBLISHED, LineupMachine.apply(LineupState.LOCKED, "unlock", true));
        assertEquals(LineupState.FINISHED, LineupMachine.apply(LineupState.LOCKED, "finish", false));
        assertThrows(BusinessException.class, () -> LineupMachine.apply(LineupState.DRAFT, "approve", true));
        assertThrows(BusinessException.class, () -> LineupMachine.apply(LineupState.DRAFT, "publish", false));
        assertThrows(BusinessException.class, () -> LineupMachine.apply(LineupState.LOCKED, "unlock", false));
    }

    @Test
    void deadlineReminderIsIdempotentByStage() {
        Instant kickoff = Instant.parse("2026-10-31T13:00:00Z");
        assertEquals("T72", LineupMachine.reminder(kickoff, kickoff.minusSeconds(70 * 3600L), false, 48));
        assertEquals("T50", LineupMachine.reminder(kickoff, kickoff.minusSeconds(49 * 3600L), false, 48));
        assertEquals("OVERDUE", LineupMachine.reminder(kickoff, kickoff.minusSeconds(47 * 3600L), false, 48));
        assertNull(LineupMachine.reminder(kickoff, kickoff.minusSeconds(47 * 3600L), true, 48));
    }

    @Test
    void ratingFormulaUsesRealMinutesAndWeights() {
        assertNull(RatingMath.system(10, 1, 0, 0, 0, false, false, false, null, null, null, null, null, null, null));
        assertEquals(new BigDecimal("7.0"), RatingMath.system(90, 1, 0, 0, 0, false, true, false, null, null, null, null, null, null, null));
        assertEquals(new BigDecimal("7.4"), RatingMath.blend(new BigDecimal("8.0"), new BigDecimal("6.0"), 70));
        FormationBook.codes().forEach(code -> assertEquals(11, FormationBook.spots(code).size(), code));
    }
}
