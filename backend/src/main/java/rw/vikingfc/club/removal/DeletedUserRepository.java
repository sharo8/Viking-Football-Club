package rw.vikingfc.club.removal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeletedUserRepository extends JpaRepository<DeletedUser, Long> {
    List<DeletedUser> findByHardDeletedFalseAndRestoreUntilAfterOrderByCreatedAtDesc(Instant instant);

    Optional<DeletedUser> findFirstByUserIdAndHardDeletedFalseOrderByCreatedAtDesc(Long userId);
}
