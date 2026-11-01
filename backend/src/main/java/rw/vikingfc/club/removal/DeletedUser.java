package rw.vikingfc.club.removal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deleted_users")
public class DeletedUser extends BaseEntity {
    private Long userId;
    @Column(nullable = false, length = 160)
    private String fullName;
    @Column(length = 180)
    private String email;
    @Column(length = 40)
    private String roleCode;
    @Column(nullable = false, length = 40)
    private String reasonCode;
    @Column(nullable = false, length = 500)
    private String reasonText;
    @Column(length = 4000)
    private String snapshotJson;
    @Column(length = 180)
    private String deletedBy;
    @Column(nullable = false)
    private boolean hardDeleted;
    private Instant restoreUntil;
}
