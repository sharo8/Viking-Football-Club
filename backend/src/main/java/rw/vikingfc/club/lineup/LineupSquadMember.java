package rw.vikingfc.club.lineup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lineup_squad_members", uniqueConstraints = @UniqueConstraint(name = "uq_lineup_squad_player", columnNames = {"lineup_id", "player_id"}))
public class LineupSquadMember extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lineup_id", nullable = false)
    private LineupVersion lineup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Column(name = "line_code", nullable = false, length = 8)
    private String lineCode;
    @Column(nullable = false)
    private boolean called;
    private Integer benchOrder;
    @Column(length = 40)
    private String reasonNotCalled;
    @Column(length = 400)
    private String overrideReason;
}
