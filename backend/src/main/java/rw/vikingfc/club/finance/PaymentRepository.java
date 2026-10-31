package rw.vikingfc.club.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInstalmentIdOrderByPaidAtAsc(Long instalmentId);
    long countByReceiptNumberStartingWith(String prefix);
}
