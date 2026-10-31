package rw.vikingfc.club.bonus;

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
import rw.vikingfc.club.season.Season;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bonus_rules")
public class BonusRule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BonusType bonusType;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;
    private LocalDate validFrom;
    private LocalDate validTo;
    @Column(name = "rule_active", nullable = false)
    private boolean ruleActive = true;
    @Column(length = 500)
    private String description;
    @Column(name = "custom_name", length = 40)
    private String customName;
}
