package rw.vikingfc.club.staff;

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
import rw.vikingfc.club.contract.ContractStatus;
import rw.vikingfc.club.season.Season;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "staff_contracts")
public class StaffContract extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Column(nullable = false)
    private int durationMonths;
    @Column(nullable = false)
    private int remainingMonths;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal seasonalPay;
    @Column(nullable = false)
    private boolean sfExempt;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal advanceAtSigning;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus contractStatus = ContractStatus.ACTIVE;
    @Column(nullable = false)
    private boolean active = true;
    private Instant deletedAt;
}
