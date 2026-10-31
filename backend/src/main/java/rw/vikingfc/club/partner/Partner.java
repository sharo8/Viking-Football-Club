package rw.vikingfc.club.partner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.common.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "partners")
public class Partner extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 400)
    private String logoUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PartnerCategory category;
    @Column(length = 160)
    private String contactName;
    @Column(length = 180)
    private String contactEmail;
    @Column(length = 40)
    private String contactPhone;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal contractValue = Money.zero();
    @Column(length = 2000)
    private String benefits;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartnerStatus status = PartnerStatus.ACTIVE;
}
