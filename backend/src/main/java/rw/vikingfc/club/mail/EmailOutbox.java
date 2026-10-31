package rw.vikingfc.club.mail;

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
@Table(name = "email_outbox")
public class EmailOutbox extends BaseEntity {
    @Column(name = "to_email", nullable = false, length = 180)
    private String toEmail;
    @Column(nullable = false, length = 200)
    private String subject;
    @Column(name = "body_html", nullable = false, length = 8000)
    private String bodyHtml;
    @Column(name = "template_name", length = 80)
    private String templateName;
    @Column(nullable = false, length = 20)
    private String status = "QUEUED";
    @Column(nullable = false)
    private int attempts;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "related_type", length = 40)
    private String relatedType;
    @Column(name = "related_id", length = 40)
    private String relatedId;
    @Column(name = "language_code", length = 8)
    private String languageCode;
    @Column(name = "sent_at")
    private Instant sentAt;
}
