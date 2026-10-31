package rw.vikingfc.club.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRoleDepartmentRepository extends JpaRepository<StaffRoleDepartment, Long> {
    Optional<StaffRoleDepartment> findByRoleNameIgnoreCase(String roleName);
}
