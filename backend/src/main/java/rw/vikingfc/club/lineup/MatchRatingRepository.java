package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRatingRepository extends JpaRepository<MatchRating, Long> {
    List<MatchRating> findByMatchId(Long matchId);
    Optional<MatchRating> findByMatchIdAndPlayerId(Long matchId, Long playerId);
    List<MatchRating> findByPlayerId(Long playerId);
}
