package rw.vikingfc.club.medical;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "injury_attachments")
public class InjuryAttachment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "injury_id", nullable = false)
    private Injury injury;
    @Column(nullable = false, length = 400)
    private String fileUrl;
    @Column(nullable = false, length = 200)
    private String fileName;
}
