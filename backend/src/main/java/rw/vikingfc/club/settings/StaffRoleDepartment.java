package rw.vikingfc.club.settings;

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
@Table(name = "staff_role_departments")
public class StaffRoleDepartment extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String roleName;
    @Column(nullable = false, length = 40)
    private String department;
}
