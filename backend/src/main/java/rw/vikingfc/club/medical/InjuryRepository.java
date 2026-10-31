package rw.vikingfc.club.medical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InjuryRepository extends JpaRepository<Injury, Long> {
    List<Injury> findByPlayerIdOrderByInjuryDateDesc(Long playerId);
    List<Injury> findByStatusNot(InjuryStatus status);
    boolean existsByPlayerIdAndStatusNot(Long playerId, InjuryStatus status);
}
