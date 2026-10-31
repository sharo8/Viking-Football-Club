package rw.vikingfc.club.auth;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "otp_codes")
public class OtpCode extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 40)
    private String purpose;
    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "ip_address", length = 64)
    private String ipAddress;
}
