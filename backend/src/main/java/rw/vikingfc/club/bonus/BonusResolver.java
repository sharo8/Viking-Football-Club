package rw.vikingfc.club.bonus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class BonusResolver {

    private BonusResolver() {
    }

    public record RuleView(Long playerId, BonusType type, BigDecimal amount, LocalDate validFrom, LocalDate validTo, boolean active) {
    }

    public static BigDecimal amount(List<RuleView> rules, Long playerId, BonusType type, LocalDate on) {
        return rules.stream()
                .filter(RuleView::active)
                .filter(rule -> rule.type() == type)
                .filter(rule -> rule.validFrom() == null || !on.isBefore(rule.validFrom()))
                .filter(rule -> rule.validTo() == null || !on.isAfter(rule.validTo()))
                .filter(rule -> rule.playerId() == null || rule.playerId().equals(playerId))
                .max(Comparator.comparing(rule -> rule.playerId() != null))
                .map(RuleView::amount)
                .orElse(BigDecimal.ZERO);
    }
}
