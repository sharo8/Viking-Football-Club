package rw.vikingfc.club.medical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClearanceNoteRepository extends JpaRepository<ClearanceNote, Long> {
    List<ClearanceNote> findByInjuryIdOrderByCreatedAtAsc(Long injuryId);
}
