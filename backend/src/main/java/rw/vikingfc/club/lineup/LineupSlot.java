package rw.vikingfc.club.lineup;

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
@Table(name = "lineup_slots")
public class LineupSlot extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lineup_id", nullable = false)
    private LineupVersion lineup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Selection selection;
    @Column(length = 40)
    private String positionSlot;
    @Column(nullable = false)
    private int sortOrder;
    @Column(nullable = false)
    private boolean captain;
    @Column(length = 400)
    private String overrideReason;
    private Integer coordX;
    private Integer coordY;
}
