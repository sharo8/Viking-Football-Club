package rw.vikingfc.club.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.player.Player;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "match_events")
public class MatchEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private FootballMatch match;
    @Column(name = "event_minute", nullable = false)
    private int eventMinute;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventType eventType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_player_id")
    private Player relatedPlayer;
    @Column(length = 400)
    private String note;
}
