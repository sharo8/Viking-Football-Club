package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineupTransitionRepository extends JpaRepository<LineupTransition, Long> {
    List<LineupTransition> findByMatchIdOrderByCreatedAtDesc(Long matchId);
}
