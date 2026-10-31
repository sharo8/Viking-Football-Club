package rw.vikingfc.club.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    List<Partner> findAllByOrderByContractValueDesc();
}
