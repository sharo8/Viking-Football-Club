package rw.vikingfc.club.sanction;

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
import rw.vikingfc.club.finance.PayMethod;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fine_payments")
public class FinePayment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fine_id", nullable = false)
    private Fine fine;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayMethod method;
    @Column(length = 80)
    private String referenceNo;
    @Column(nullable = false)
    private Instant paidAt;
    @Column(length = 120)
    private String receivedBy;
}
