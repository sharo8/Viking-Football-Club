package rw.vikingfc.club.bonus;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.util.Money;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BonusCalculationTest {

    @Test
    void playerRuleOverridesTheGlobalAmount() {
        LocalDate today = LocalDate.of(2026, 10, 24);
        List<BonusResolver.RuleView> rules = List.of(
                new BonusResolver.RuleView(null, BonusType.GOAL, Money.of(20000), today.minusDays(10), today.plusDays(10), true),
                new BonusResolver.RuleView(8L, BonusType.GOAL, Money.of(50000), today.minusDays(10), today.plusDays(10), true)
        );
        assertEquals(Money.of(50000), BonusResolver.amount(rules, 8L, BonusType.GOAL, today));
        assertEquals(Money.of(20000), BonusResolver.amount(rules, 2L, BonusType.GOAL, today));
        assertEquals(Money.zero(), BonusResolver.amount(rules, 2L, BonusType.ASSIST, today));
    }
}
