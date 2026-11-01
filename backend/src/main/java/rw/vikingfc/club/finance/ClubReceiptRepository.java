package rw.vikingfc.club.finance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubReceiptRepository extends JpaRepository<ClubReceipt, Long> {
    long countByReceiptNumberStartingWith(String prefix);
    java.util.Optional<ClubReceipt> findByReceiptNumber(String receiptNumber);
    java.util.List<ClubReceipt> findByPersonTypeAndPersonIdOrderByCreatedAtDesc(String personType, Long personId);
}
