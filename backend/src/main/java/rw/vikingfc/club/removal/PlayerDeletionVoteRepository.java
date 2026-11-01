package rw.vikingfc.club.removal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerDeletionVoteRepository extends JpaRepository<PlayerDeletionVote, Long> {
    List<PlayerDeletionVote> findByRequestIdOrderByDecidedAtAsc(Long requestId);

    boolean existsByRequestIdAndVoterRole(Long requestId, String voterRole);
}
