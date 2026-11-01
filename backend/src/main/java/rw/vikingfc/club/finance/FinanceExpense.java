package rw.vikingfc.club.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "finance_expenses")
public class FinanceExpense extends BaseEntity {
    @Column(nullable = false, length = 80)
    private String category;
    @Column(length = 80)
    private String subcategory;
    @Column(name = "custom_label", length = 80)
    private String customLabel;
    @Column(nullable = false, length = 400)
    private String description;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDate expenseDate;
    @Column(length = 160)
    private String vendorName;
    @Column(length = 40)
    private String vendorPhone;
    @Column(length = 30)
    private String method;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(length = 500)
    private String note;
    @Column(length = 160)
    private String linkedLabel;
    @Column(nullable = false)
    private boolean reversed;
    @Column(length = 400)
    private String reverseReason;
}
