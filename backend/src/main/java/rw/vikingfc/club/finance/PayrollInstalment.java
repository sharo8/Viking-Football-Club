package rw.vikingfc.club.finance;

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
import rw.vikingfc.club.season.Season;
import rw.vikingfc.club.staff.StaffMember;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_instalments")
public class PayrollInstalment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private StaffMember staff;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InstalmentKind kind;
    @Column(nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amountDue = Money.zero();
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amountPaid = Money.zero();
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amountDeducted = Money.zero();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstalmentStatus status = InstalmentStatus.NOT_PAID;
    private LocalDate paymentDate;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PayMethod method;
    @Column(length = 80)
    private String referenceNo;
    @Column(length = 120)
    private String paidBy;
    @Column(length = 40)
    private String receiptNumber;

    public BigDecimal cashRemaining() {
        return Money.nz(amountDue).subtract(Money.nz(amountPaid)).subtract(Money.nz(amountDeducted));
    }
}
