package rw.vikingfc.club.mail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    List<EmailOutbox> findByStatusOrderByCreatedAtDesc(String status);
    List<EmailOutbox> findAllByOrderByCreatedAtDesc();
    List<EmailOutbox> findByStatusInAndAttemptsLessThan(List<String> statuses, int attempts);
    List<EmailOutbox> findByRelatedTypeAndRelatedIdOrderByCreatedAtDesc(String relatedType, String relatedId);
}
