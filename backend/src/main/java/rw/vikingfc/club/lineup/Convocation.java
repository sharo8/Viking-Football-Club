package rw.vikingfc.club.lineup;

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
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.player.Player;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "convocations")
public class Convocation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private FootballMatch match;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Column(nullable = false)
    private boolean called;
    @Column(nullable = false, length = 20)
    private String response = "PENDING";
    @Column(length = 40)
    private String reason;
    @Column(length = 400)
    private String reasonNote;
    @Column(length = 80)
    private String token;
    private Instant tokenExpires;
    @Column(nullable = false)
    private int attempts;
    private Instant notifiedAt;
    private Instant readAt;
    private Instant confirmedAt;
}
