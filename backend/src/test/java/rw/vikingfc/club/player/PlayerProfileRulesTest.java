package rw.vikingfc.club.player;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileRulesTest {

    @Test
    void aNewPlayerIsBlockedUntilEverySectionIsComplete() {
        var issues = PlayerProfileRules.issues(new HashMap<>(), true, true, false, LocalDate.of(2026, 10, 25));
        assertTrue(issues.stream().anyMatch(item -> "photo".equals(item.get("field"))));
        assertTrue(PlayerProfileRules.blocking(issues, true).size() > 10);
    }

    @Test
    void aLegacyPlayerCanBeSavedWhenPresentValuesAreValid() {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Legacy Player");
        body.put("email", "not-an-email");
        var issues = PlayerProfileRules.issues(body, false, true, false, LocalDate.of(2026, 10, 25));
        assertTrue(issues.stream().anyMatch(item -> "EMAIL".equals(item.get("code"))));
        assertTrue(PlayerProfileRules.blocking(issues, false).stream().noneMatch(item -> "REQUIRED".equals(item.get("code"))));
        assertEquals(1, PlayerProfileRules.blocking(issues, false).size());
    }

    @Test
    void advanceCannotExceedSeasonalPay() {
        Map<String, Object> body = new HashMap<>();
        body.put("seasonalPay", 240000);
        body.put("advance", 300000);
        var issues = PlayerProfileRules.issues(body, false, true, true, LocalDate.of(2026, 10, 25));
        assertTrue(issues.stream().anyMatch(item -> "ADVANCE".equals(item.get("code"))));
    }
}
