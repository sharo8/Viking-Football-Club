package rw.vikingfc.club.removal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerDeletionRequestRepository extends JpaRepository<PlayerDeletionRequest, Long> {
    Optional<PlayerDeletionRequest> findFirstByPlayerIdAndStatus(Long playerId, String status);

    List<PlayerDeletionRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<PlayerDeletionRequest> findByPlayerIdOrderByCreatedAtDesc(Long playerId);

    long countByStatus(String status);
}
