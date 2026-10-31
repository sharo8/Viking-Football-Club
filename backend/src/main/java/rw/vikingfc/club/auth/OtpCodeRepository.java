package rw.vikingfc.club.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    List<OtpCode> findByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(Long userId, String purpose);
    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, String purpose, Instant after);
    Optional<OtpCode> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(Long userId, String purpose);
}
