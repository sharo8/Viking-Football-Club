package rw.vikingfc.club.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface OpponentRepository extends JpaRepository<Opponent, Long> {
    List<Opponent> findAllByOrderByNameAsc();
    Optional<Opponent> findByNameIgnoreCase(String name);
}
