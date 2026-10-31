package rw.vikingfc.club.marketing;

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
import rw.vikingfc.club.user.User;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 1000)
    private String body;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(length = 300)
    private String link;
    @Column(length = 2000)
    private String payload;
    @Column(nullable = false)
    private boolean readFlag;
    private Instant readAt;
}
