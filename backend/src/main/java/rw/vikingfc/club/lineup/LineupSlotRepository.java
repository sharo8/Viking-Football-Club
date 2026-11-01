package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineupSlotRepository extends JpaRepository<LineupSlot, Long> {
    List<LineupSlot> findByLineupIdOrderBySortOrderAsc(Long lineupId);
    void deleteByLineupId(Long lineupId);

    List<LineupSlot> findByPlayerId(Long playerId);
}
