package rw.vikingfc.club.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "finance_receipts")
public class ClubReceipt extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String receiptNumber;
    @Column(nullable = false, length = 20)
    private String personType;
    @Column(nullable = false)
    private Long personId;
    @Column(nullable = false, length = 160)
    private String personName;
    private Long paymentId;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(length = 160)
    private String financeSigner;
    @Column(length = 160)
    private String recipientSigner;
    private Instant signedFinanceAt;
    private Instant signedRecipientAt;
}
