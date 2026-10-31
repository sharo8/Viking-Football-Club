package rw.vikingfc.club.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchPlayerRecordRepository extends JpaRepository<MatchPlayerRecord, Long> {
    List<MatchPlayerRecord> findByMatchId(Long matchId);
    List<MatchPlayerRecord> findByPlayerId(Long playerId);
    Optional<MatchPlayerRecord> findByMatchIdAndPlayerId(Long matchId, Long playerId);
    void deleteByMatchId(Long matchId);
}
