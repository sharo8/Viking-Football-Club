package rw.vikingfc.club.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceReportRunRepository extends JpaRepository<FinanceReportRun, Long> {
    List<FinanceReportRun> findTop30ByOrderByCreatedAtDesc();
}
