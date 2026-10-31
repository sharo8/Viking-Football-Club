package rw.vikingfc.club.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootballMatchRepository extends JpaRepository<FootballMatch, Long> {
    List<FootballMatch> findBySeasonIdOrderByKickoffAsc(Long seasonId);
    List<FootballMatch> findBySeasonIdAndStatusOrderByKickoffDesc(Long seasonId, MatchStatus status);
}
