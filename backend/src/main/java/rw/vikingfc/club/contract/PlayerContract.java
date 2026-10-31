package rw.vikingfc.club.contract;

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
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_contracts")
public class PlayerContract extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Column(nullable = false)
    private int durationYears;
    @Column(nullable = false)
    private int durationMonths;
    @Column(nullable = false)
    private int remainingMonths;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal seasonalPay;
    @Column(nullable = false)
    private boolean sfExempt;
    @Column(name = "social_fund_amount", precision = 14, scale = 0)
    private BigDecimal socialFundAmount;

    public BigDecimal socialFundOr(BigDecimal clubDefault) {
        return socialFundAmount != null ? socialFundAmount : clubDefault;
    }
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal advanceAtSigning;
    @Column(precision = 14, scale = 0)
    private BigDecimal releaseClauseAmount;
    @Column(nullable = false, length = 8)
    private String releaseClauseCurrency = "RWF";
    @Column(nullable = false)
    private boolean releaseClauseActive;
    @Column(precision = 14, scale = 0)
    private BigDecimal marketValue;
    @Column(length = 160)
    private String agentName;
    @Column(precision = 14, scale = 0)
    private BigDecimal agentCommission;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus contractStatus = ContractStatus.ACTIVE;
    @Column(nullable = false)
    private boolean active = true;
    private Instant deletedAt;
}
