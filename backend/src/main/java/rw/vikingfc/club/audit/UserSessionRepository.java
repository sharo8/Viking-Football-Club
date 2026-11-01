package rw.vikingfc.club.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(Long userId);
}
