package rw.vikingfc.club.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRoleRepository extends JpaRepository<ClubRole, Long> {
    Optional<ClubRole> findByCode(String code);
}
