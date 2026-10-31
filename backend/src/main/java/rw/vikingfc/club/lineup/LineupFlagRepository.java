package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineupFlagRepository extends JpaRepository<LineupFlag, Long> {
    List<LineupFlag> findByMatchIdOrderByCreatedAtDesc(Long matchId);
}
