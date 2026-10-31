package rw.vikingfc.club.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, Long> {
    List<Standing> findBySeasonIdOrderByPositionNoAsc(Long seasonId);
    Optional<Standing> findBySeasonIdAndTeamName(Long seasonId, String teamName);
}
