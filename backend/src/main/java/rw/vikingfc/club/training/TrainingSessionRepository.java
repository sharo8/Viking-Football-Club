package rw.vikingfc.club.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    List<TrainingSession> findBySeasonIdOrderBySessionDateAsc(Long seasonId);
    Optional<TrainingSession> findBySessionDate(LocalDate sessionDate);
    long countBySeasonId(Long seasonId);
}
