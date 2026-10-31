package rw.vikingfc.club.transfer;

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
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.player.Player;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transfers")
public class Transfer extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;
    @Column(nullable = false, length = 160)
    private String playerName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferType transferType;
    @Column(length = 160)
    private String fromClub;
    @Column(length = 160)
    private String toClub;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal fee = Money.zero();
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal agentFee = Money.zero();
    private LocalDate transferDate;
    private Integer contractMonths;
    @Column(precision = 14, scale = 0)
    private BigDecimal seasonalPay;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status = TransferStatus.NEGOTIATING;
    @Column(length = 2000)
    private String notes;
}
