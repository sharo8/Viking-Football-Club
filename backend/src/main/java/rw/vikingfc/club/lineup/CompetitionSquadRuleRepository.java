package rw.vikingfc.club.lineup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionSquadRuleRepository extends JpaRepository<CompetitionSquadRule, Long> {
    Optional<CompetitionSquadRule> findByCompetitionIgnoreCase(String competition);
}
