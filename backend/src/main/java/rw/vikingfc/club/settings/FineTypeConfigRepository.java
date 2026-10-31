package rw.vikingfc.club.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FineTypeConfigRepository extends JpaRepository<FineTypeConfig, Long> {
    Optional<FineTypeConfig> findByCode(String code);
}
