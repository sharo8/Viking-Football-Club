package rw.vikingfc.club.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "players")
public class Player extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Integer registerNo;

    private Integer jerseyNumber;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, length = 80)
    private String knownAs;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_position", nullable = false, length = 40)
    private Position position;

    @Column(nullable = false, length = 40)
    private String lineName;

    private LocalDate dateOfBirth;
    @Column(length = 80)
    private String nationality;
    @Column(length = 40)
    private String phone;
    @Column(length = 180)
    private String email;
    @Column(length = 20)
    private String preferredFoot;
    private Integer heightCm;
    private Integer weightKg;
    @Column(length = 400)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerStatus status = PlayerStatus.ACTIVE;

    @Column(nullable = false)
    private boolean newSigning;
    private LocalDate signedOn;
    @Column(length = 160)
    private String emergencyContactName;
    @Column(length = 40)
    private String emergencyContactPhone;
    @Column(name = "emergency_contact_relationship", length = 80)
    private String emergencyContactRelationship;
    @Column(name = "emergency_contact_email", length = 180)
    private String emergencyContactEmail;
    @Column(name = "preferred_language", length = 8)
    private String preferredLanguage = "en";
    @Column(length = 200)
    private String address;
    @Column(length = 2000)
    private String notes;
    @Column(nullable = false)
    private boolean active = true;
    private Instant deletedAt;
}
