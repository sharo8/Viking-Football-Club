package rw.vikingfc.club.staff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffContractRepository extends JpaRepository<StaffContract, Long> {
    List<StaffContract> findBySeasonIdAndActiveTrue(Long seasonId);
    Optional<StaffContract> findByStaffIdAndSeasonIdAndActiveTrue(Long staffId, Long seasonId);
}
