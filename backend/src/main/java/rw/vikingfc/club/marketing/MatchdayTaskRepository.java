package rw.vikingfc.club.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchdayTaskRepository extends JpaRepository<MatchdayTask, Long> {
    List<MatchdayTask> findByMatchId(Long matchId);
}
