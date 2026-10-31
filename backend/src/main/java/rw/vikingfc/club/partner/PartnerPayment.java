package rw.vikingfc.club.partner;

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
import rw.vikingfc.club.finance.InstalmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "partner_payments")
public class PartnerPayment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;
    @Column(nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amountDue;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amountPaid;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstalmentStatus status;
    @Column(length = 80)
    private String referenceNo;
}
