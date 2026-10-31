package rw.vikingfc.club.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubSettingsRepository extends JpaRepository<ClubSettings, Long> {
    Optional<ClubSettings> findBySeasonId(Long seasonId);
}
