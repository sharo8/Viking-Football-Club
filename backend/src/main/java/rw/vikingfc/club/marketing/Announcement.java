package rw.vikingfc.club.marketing;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.user.Role;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "announcements")
public class Announcement extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 4000)
    private String body;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "announcement_audiences", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "role", length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Role> audiences = new HashSet<>();
}
