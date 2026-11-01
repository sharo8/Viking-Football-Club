package rw.vikingfc.club.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByActiveTrueOrderByRegisterNoAsc();

    Optional<Player> findByIdAndActiveTrue(Long id);

    Optional<Player> findByRegisterNoAndActiveTrue(Integer registerNo);

    @Query("""
            select p.jerseyNumber from Player p
            where p.active = true and p.jerseyNumber is not null
            group by p.jerseyNumber
            having count(p) > 1
            """)
    List<Integer> findDuplicateJerseys();

    List<Player> findByActiveTrueAndNewSigningTrueOrderBySignedOnDesc();

    long countByActiveTrue();

    Optional<Player> findFirstByUserId(Long userId);

    List<Player> findByStatusAndActiveTrueOrderByFullNameAsc(PlayerStatus status);
}
