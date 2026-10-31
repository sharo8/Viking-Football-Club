package rw.vikingfc.club.bonus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BonusRuleRepository extends JpaRepository<BonusRule, Long> {
    List<BonusRule> findBySeasonId(Long seasonId);
    List<BonusRule> findByPlayerIdAndSeasonId(Long playerId, Long seasonId);
}
