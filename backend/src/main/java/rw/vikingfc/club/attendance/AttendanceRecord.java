package rw.vikingfc.club.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.training.TrainingSession;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_records", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "player_id"}))
public class AttendanceRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TrainingSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private AttendanceMark status;
    private Integer minutesLate;
    @Column(nullable = false)
    private boolean justified;
    @Column(length = 500)
    private String note;
    @Column(length = 120)
    private String recordedBy;
}
