package rw.vikingfc.club.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.season.Season;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "training_sessions")
public class TrainingSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(nullable = false)
    private LocalDate sessionDate;
    @Column(nullable = false, length = 12)
    private String weekday;
    @Column(nullable = false)
    private int weekNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionType sessionType;
    private LocalTime startTime;
    @Column(length = 200)
    private String location;
    @Column(length = 1000)
    private String notes;
}
