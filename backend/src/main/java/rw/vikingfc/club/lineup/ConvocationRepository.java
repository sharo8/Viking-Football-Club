package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConvocationRepository extends JpaRepository<Convocation, Long> {
    List<Convocation> findByMatchId(Long matchId);
    Optional<Convocation> findByMatchIdAndPlayerId(Long matchId, Long playerId);
    Optional<Convocation> findByToken(String token);
}
