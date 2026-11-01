package rw.vikingfc.club.competition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    List<Competition> findBySeasonIdAndArchivedFalseOrderByNameAsc(Long seasonId);
    Optional<Competition> findBySeasonIdAndNameIgnoreCase(Long seasonId, String name);
    List<Competition> findBySeasonIdAndTypeAndArchivedFalse(Long seasonId, CompetitionType type);
}
