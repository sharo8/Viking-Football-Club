package rw.vikingfc.club.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "birthday_notification_log")
public class BirthdayNotice extends BaseEntity {
    @Column(name = "player_id", nullable = false)
    private Long playerId;
    @Column(name = "year_number", nullable = false)
    private int yearNumber;
    @Column(name = "notice_type", nullable = false, length = 8)
    private String noticeType;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
