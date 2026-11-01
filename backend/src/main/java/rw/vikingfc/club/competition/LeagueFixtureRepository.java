package rw.vikingfc.club.competition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LeagueFixtureRepository extends JpaRepository<LeagueFixture, Long> {
    List<LeagueFixture> findByCompetitionIdOrderByKickoffAsc(Long competitionId);
    List<LeagueFixture> findByKickoffBetweenOrderByKickoffAsc(Instant from, Instant to);
}
