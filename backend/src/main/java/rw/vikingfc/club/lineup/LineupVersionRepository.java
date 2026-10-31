package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineupVersionRepository extends JpaRepository<LineupVersion, Long> {
    List<LineupVersion> findByMatchIdOrderByRevisionDesc(Long matchId);
    Optional<LineupVersion> findFirstByMatchIdAndStatusOrderByRevisionDesc(Long matchId, LineupState status);
}
