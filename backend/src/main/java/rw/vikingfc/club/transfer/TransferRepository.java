package rw.vikingfc.club.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByPlayerIdOrderByTransferDateDesc(Long playerId);
    List<Transfer> findAllByOrderByTransferDateDesc();
}
