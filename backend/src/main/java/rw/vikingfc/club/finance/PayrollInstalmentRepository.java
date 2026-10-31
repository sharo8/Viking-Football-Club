package rw.vikingfc.club.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollInstalmentRepository extends JpaRepository<PayrollInstalment, Long> {
    boolean existsByPlayer_Id(Long playerId);
    boolean existsByStaff_Id(Long staffId);
    List<PayrollInstalment> findByPlayerIdOrderByDueDateAsc(Long playerId);
    List<PayrollInstalment> findByStaffIdOrderByDueDateAsc(Long staffId);
    List<PayrollInstalment> findBySeasonId(Long seasonId);
    List<PayrollInstalment> findByStatusIn(List<InstalmentStatus> statuses);
}
