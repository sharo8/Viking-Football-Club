package rw.vikingfc.club.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    boolean existsByMatchIdAndWindowKeyAndRecipientEmailAndChannel(Long matchId, String windowKey, String email, String channel);
    Optional<AlertLog> findFirstByMatchIdAndWindowKeyAndRecipientEmailAndChannel(Long matchId, String windowKey, String email, String channel);
}
