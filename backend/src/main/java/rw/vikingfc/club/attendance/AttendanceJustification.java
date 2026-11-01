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
import rw.vikingfc.club.training.TrainingSession;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_justifications")
public class AttendanceJustification extends BaseEntity {
    @Column(nullable = false, length = 20)
    private String personType;
    @Column(nullable = false)
    private Long personId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TrainingSession session;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(length = 200)
    private String evidenceName;
    @Column(length = 160)
    private String submittedBy;
}
