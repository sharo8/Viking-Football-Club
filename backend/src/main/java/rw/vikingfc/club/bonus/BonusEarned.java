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
import rw.vikingfc.club.finance.Payment;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.player.Player;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bonuses_earned")
public class BonusEarned extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private FootballMatch match;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private BonusRule rule;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BonusType bonusType;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BonusStatus status = BonusStatus.PENDING;
    @Column(length = 120)
    private String approvedBy;
    private Instant approvedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;
    @Column(nullable = false)
    private boolean manualEntry;
    @Column(length = 500)
    private String description;
    @Column(length = 140, unique = true)
    private String sourceKey;
    private LocalDate earnedOn;
}
