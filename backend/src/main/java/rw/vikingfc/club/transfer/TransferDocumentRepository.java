package rw.vikingfc.club.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferDocumentRepository extends JpaRepository<TransferDocument, Long> {
    List<TransferDocument> findByTransferId(Long transferId);
}
