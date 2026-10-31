package rw.vikingfc.club.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
    List<MatchEvent> findByMatchIdOrderByEventMinuteAsc(Long matchId);
    List<MatchEvent> findByEventType(EventType type);
}
