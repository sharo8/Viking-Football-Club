package rw.vikingfc.club.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "finance_report_runs")
public class FinanceReportRun extends BaseEntity {
    @Column(nullable = false, length = 40)
    private String reportWhat;
    @Column(nullable = false, length = 40)
    private String reportWho;
    @Column(nullable = false, length = 80)
    private String periodLabel;
    @Column(nullable = false, length = 20)
    private String splitMode;
    @Column(nullable = false, length = 20)
    private String detailLevel;
    @Column(nullable = false, length = 10)
    private String format;
    @Column(nullable = false)
    private long byteSize;
}
