package rw.vikingfc.club.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingAdjustmentRepository extends JpaRepository<StandingAdjustment, Long> {
    List<StandingAdjustment> findByCompetitionId(Long competitionId);
    Optional<StandingAdjustment> findByCompetitionIdAndTeamNameIgnoreCase(Long competitionId, String teamName);
}
