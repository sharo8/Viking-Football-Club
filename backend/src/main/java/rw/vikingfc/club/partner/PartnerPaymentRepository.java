package rw.vikingfc.club.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerPaymentRepository extends JpaRepository<PartnerPayment, Long> {
    List<PartnerPayment> findByPartnerIdOrderByDueDateAsc(Long partnerId);
}
