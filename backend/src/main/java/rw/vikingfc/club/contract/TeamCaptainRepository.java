package rw.vikingfc.club.contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamCaptainRepository extends JpaRepository<TeamCaptain, Long> {
    List<TeamCaptain> findBySeasonIdOrderByRankAsc(Long seasonId);
    List<TeamCaptain> findBySeasonIdAndEndedAtIsNullOrderByRankAsc(Long seasonId);
    Optional<TeamCaptain> findBySeasonIdAndRankAndEndedAtIsNull(Long seasonId, CaptainRank rank);
    Optional<TeamCaptain> findBySeasonIdAndPlayerIdAndEndedAtIsNull(Long seasonId, Long playerId);

    List<TeamCaptain> findByPlayerIdAndEndedAtIsNull(Long playerId);
    Optional<TeamCaptain> findBySeasonIdAndRank(Long seasonId, CaptainRank rank);
    Optional<TeamCaptain> findBySeasonIdAndPlayerId(Long seasonId, Long playerId);
    long countBySeasonId(Long seasonId);
}
