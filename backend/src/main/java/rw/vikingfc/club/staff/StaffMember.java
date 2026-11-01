package rw.vikingfc.club.staff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.user.User;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "staff_members")
public class StaffMember extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String fullName;
    @Column(nullable = false, length = 80)
    private String roleName;

    @Column(name = "listed_in", nullable = false, length = 20)
    private String listedIn = "STAFF";
    @Column(nullable = false, length = 40)
    private String department;
    @Column(length = 40)
    private String phone;
    @Column(length = 180)
    private String email;
    private LocalDate dateOfBirth;
    @Column(name = "job_title", length = 80)
    private String jobTitle;
    @Column(length = 400)
    private String photoUrl;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(length = 2000)
    private String notes;
    @Column(nullable = false)
    private boolean active = true;
    private Instant deletedAt;
}
