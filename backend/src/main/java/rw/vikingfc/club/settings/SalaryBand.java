package rw.vikingfc.club.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "salary_bands")
public class SalaryBand extends BaseEntity {
    @Column(nullable = false, length = 40)
    private String label;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal minAmount;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal maxAmount;
    @Column(nullable = false)
    private int sortOrder;
}
