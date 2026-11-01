package rw.vikingfc.club.competition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionTeamRepository extends JpaRepository<CompetitionTeam, Long> {
    List<CompetitionTeam> findByCompetitionId(Long competitionId);
    Optional<CompetitionTeam> findByCompetitionIdAndOpponentId(Long competitionId, Long opponentId);
    void deleteByCompetitionIdAndOpponentId(Long competitionId, Long opponentId);
}
