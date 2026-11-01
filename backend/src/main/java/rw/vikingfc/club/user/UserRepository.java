package rw.vikingfc.club.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findFirstByPhoneAndDeletedAtIsNull(String phone);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByDeletedAtIsNullOrderByFullNameAsc();

    List<User> findByRoleInAndActiveTrueAndDeletedAtIsNull(Collection<Role> roles);

    long countByRoleInAndActiveTrueAndDeletedAtIsNull(Collection<Role> roles);

    long countByRoleAndActiveTrueAndDeletedAtIsNull(Role role);

    long countByRoleCodeAndDeletedAtIsNull(String roleCode);

    List<User> findByRoleCodeAndDeletedAtIsNull(String roleCode);
}
