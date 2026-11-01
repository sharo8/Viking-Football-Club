package rw.vikingfc.club.medical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MedicalCheckRepository extends JpaRepository<MedicalCheck, Long> {
    List<MedicalCheck> findByInjuryIdOrderByCheckDateAsc(Long injuryId);
    List<MedicalCheck> findByCheckDateBetween(LocalDate from, LocalDate to);
}
