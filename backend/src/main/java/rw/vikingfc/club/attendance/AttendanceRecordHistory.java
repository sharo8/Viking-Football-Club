package rw.vikingfc.club.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_record_history")
public class AttendanceRecordHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private AttendanceRecord record;
    @Column(length = 2)
    private String oldStatus;
    @Column(nullable = false, length = 2)
    private String newStatus;
    private Integer minutesLate;
    @Column(length = 400)
    private String reason;
    @Column(nullable = false, length = 160)
    private String changedBy;
    @Column(nullable = false)
    private Instant changedAt;
}
