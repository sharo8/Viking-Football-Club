package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LineupReminderRepository extends JpaRepository<LineupReminder, Long> {
    boolean existsByMatchIdAndKind(Long matchId, String kind);
}
