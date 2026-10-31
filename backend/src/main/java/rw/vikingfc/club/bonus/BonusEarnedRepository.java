package rw.vikingfc.club.bonus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BonusEarnedRepository extends JpaRepository<BonusEarned, Long> {
    Optional<BonusEarned> findBySourceKey(String sourceKey);
    List<BonusEarned> findByPlayerIdOrderByEarnedOnDesc(Long playerId);
    List<BonusEarned> findByStatus(BonusStatus status);
    List<BonusEarned> findByMatchId(Long matchId);
}
