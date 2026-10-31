package rw.vikingfc.club.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryBandRepository extends JpaRepository<SalaryBand, Long> {
    List<SalaryBand> findAllByOrderBySortOrderAsc();
}
