package rw.vikingfc.club.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PositionLineMappingRepository extends JpaRepository<PositionLineMapping, Long> {
    Optional<PositionLineMapping> findByPositionCode(String positionCode);
}
