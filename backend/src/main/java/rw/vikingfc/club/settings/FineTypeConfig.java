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
@Table(name = "fine_type_configs")
public class FineTypeConfig extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 120)
    private String label;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal defaultAmount;
    private Integer lateAfterMinutes;
    @Column(nullable = false, length = 20)
    private String category;
    @Column(nullable = false)
    private boolean active = true;
}
