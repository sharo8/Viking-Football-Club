package rw.vikingfc.club.staff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {
    List<StaffMember> findByActiveTrueOrderByFullNameAsc();
    Optional<StaffMember> findByIdAndActiveTrue(Long id);
}
