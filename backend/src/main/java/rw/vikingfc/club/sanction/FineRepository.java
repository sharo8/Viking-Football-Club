package rw.vikingfc.club.sanction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {
    Optional<Fine> findByAttendanceRecordId(Long attendanceRecordId);
    List<Fine> findByPlayerIdOrderByFineDateAsc(Long playerId);
    List<Fine> findByMatchIdAndPlayerIdAndFineType(Long matchId, Long playerId, FineType type);
}
