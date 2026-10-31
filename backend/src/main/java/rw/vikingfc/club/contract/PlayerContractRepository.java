package rw.vikingfc.club.contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlayerContractRepository extends JpaRepository<PlayerContract, Long> {
    Optional<PlayerContract> findByPlayerIdAndSeasonIdAndActiveTrue(Long playerId, Long seasonId);
    List<PlayerContract> findBySeasonIdAndActiveTrue(Long seasonId);
    List<PlayerContract> findByActiveTrueAndContractStatusNotAndEndDateLessThanEqual(ContractStatus status, LocalDate end);
}
