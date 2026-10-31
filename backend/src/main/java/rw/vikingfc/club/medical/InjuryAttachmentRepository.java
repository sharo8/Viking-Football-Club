package rw.vikingfc.club.medical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InjuryAttachmentRepository extends JpaRepository<InjuryAttachment, Long> {
    List<InjuryAttachment> findByInjuryId(Long injuryId);
}
