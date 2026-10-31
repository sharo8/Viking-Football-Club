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
@Table(name = "position_line_mappings")
public class PositionLineMapping extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String positionCode;
    @Column(nullable = false, length = 60)
    private String positionLabel;
    @Column(nullable = false, length = 40)
    private String lineName;
}
