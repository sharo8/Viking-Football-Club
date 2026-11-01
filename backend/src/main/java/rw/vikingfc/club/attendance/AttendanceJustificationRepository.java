package rw.vikingfc.club.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceJustificationRepository extends JpaRepository<AttendanceJustification, Long> {
    List<AttendanceJustification> findByPersonTypeAndPersonIdOrderByCreatedAtDesc(String personType, Long personId);
    List<AttendanceJustification> findBySession_Id(Long sessionId);
}
