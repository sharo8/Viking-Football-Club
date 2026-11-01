package rw.vikingfc.club.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "club_roles")
public class ClubRole extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "name_en", nullable = false, length = 80)
    private String nameEn;

    @Column(name = "name_fr", nullable = false, length = 80)
    private String nameFr;

    @Column(name = "name_rw", nullable = false, length = 80)
    private String nameRw;

    /** Language of the name that was typed. The other languages are optional. */
    @Column(name = "source_lang", length = 2)
    private String sourceLang = "en";

    @Column(length = 400)
    private String description;

    @Column(nullable = false, length = 12)
    private String color = "#a3283a";

    @Column(name = "otp_required", nullable = false)
    private boolean otpRequired;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole = true;

    @Column(name = "max_seats")
    private Integer maxSeats;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "permissions_json", length = 4000)
    private String permissionsJson;
}
