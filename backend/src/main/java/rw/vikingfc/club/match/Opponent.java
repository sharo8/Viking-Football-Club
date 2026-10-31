package rw.vikingfc.club.match;

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
@Table(name = "opponents")
public class Opponent extends BaseEntity {
    @Column(nullable = false, unique = true, length = 160)
    private String name;
    @Column(length = 40)
    private String shortName;
    @Column(length = 400)
    private String logoUrl;
    @Column(length = 80)
    private String city;
}
