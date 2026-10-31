package rw.vikingfc.club.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySessionId(Long sessionId);
    List<AttendanceRecord> findByPlayerId(Long playerId);
    Optional<AttendanceRecord> findBySessionIdAndPlayerId(Long sessionId, Long playerId);
    List<AttendanceRecord> findBySession_Season_Id(Long seasonId);
}
