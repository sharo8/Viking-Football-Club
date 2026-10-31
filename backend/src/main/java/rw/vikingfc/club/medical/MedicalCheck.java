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
import rw.vikingfc.club.user.User;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medical_checks")
public class MedicalCheck extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "injury_id", nullable = false)
    private Injury injury;
    @Column(nullable = false)
    private LocalDate checkDate;
    @Column(nullable = false, length = 2000)
    private String note;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InjuryStatus statusAfter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_user_id")
    private User doctor;
}
