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
import rw.vikingfc.club.user.User;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instalment_id", nullable = false)
    private PayrollInstalment instalment;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayMethod method;
    @Column(length = 80)
    private String referenceNo;
    @Column(nullable = false)
    private Instant paidAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id")
    private User paidByUser;
    @Column(length = 40)
    private String receiptNumber;
    @Column(nullable = false)
    private boolean reversed;
    private Instant reversedAt;
    @Column(length = 400)
    private String reverseReason;
    @Column(length = 120)
    private String reversedBy;
}
