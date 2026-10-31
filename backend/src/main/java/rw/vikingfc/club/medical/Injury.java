package rw.vikingfc.club.medical;

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
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.training.TrainingSession;
import rw.vikingfc.club.user.User;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "injuries")
public class Injury extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Column(nullable = false, length = 120)
    private String injuryType;
    @Column(length = 500)
    private String diagnosis;
    @Column(length = 80)
    private String bodyPart;
    @Column(length = 20)
    private String side;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InjurySeverity severity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InjuryCause cause;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private FootballMatch match;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_session_id")
    private TrainingSession trainingSession;
    @Column(nullable = false)
    private LocalDate injuryDate;
    private LocalDate expectedReturnDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InjuryStatus status;
    @Column(length = 2000)
    private String treatmentPlan;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_user_id")
    private User doctor;
    @Column(length = 4000)
    private String confidentialNotes;
    private LocalDate clearedOn;
    @Column(length = 120)
    private String clearedBy;
}
