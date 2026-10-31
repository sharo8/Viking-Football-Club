package rw.vikingfc.club.marketing;

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
@Table(name = "campaigns")
public class Campaign extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal budget = Money.zero();
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(length = 500)
    private String goal;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;
    @Column(length = 2000)
    private String resultsNote;
    private Long reach;
    private Long engagement;
}
