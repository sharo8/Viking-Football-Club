package rw.vikingfc.club.sanction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinePaymentRepository extends JpaRepository<FinePayment, Long> {
    List<FinePayment> findByFineId(Long fineId);
}
