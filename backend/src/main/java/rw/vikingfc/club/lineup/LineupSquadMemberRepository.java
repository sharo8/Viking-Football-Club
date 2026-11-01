package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineupSquadMemberRepository extends JpaRepository<LineupSquadMember, Long> {
    List<LineupSquadMember> findByLineupIdOrderByBenchOrderAsc(Long lineupId);

    void deleteByLineupId(Long lineupId);

    List<LineupSquadMember> findByPlayerId(Long playerId);
}
