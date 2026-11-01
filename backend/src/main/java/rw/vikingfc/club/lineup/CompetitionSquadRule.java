package rw.vikingfc.club.lineup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "competition_squad_rules")
public class CompetitionSquadRule extends BaseEntity {
    @Column(nullable = false, unique = true, length = 160)
    private String competition;
    @Column(nullable = false)
    private int squadSize;
    @Column(nullable = false)
    private int maxBench;
    @Column(nullable = false)
    private int minGoalkeepers = 2;
}
