package rw.vikingfc.club.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRecordHistoryRepository extends JpaRepository<AttendanceRecordHistory, Long> {
    List<AttendanceRecordHistory> findByRecord_IdOrderByChangedAtAsc(Long recordId);
}
