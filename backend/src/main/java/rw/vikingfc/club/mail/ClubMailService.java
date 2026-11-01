package rw.vikingfc.club.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.config.MailSettings;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMailService {

    private final EmailOutboxRepository outboxRepository;
    private final MailSettings settings;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final ApplicationContext applicationContext;
    @Value("${spring.mail.username:}")
    private String smtpUsername;
    @Value("${spring.mail.host:}")
    private String smtpHost;
    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @PostConstruct
    void reportMail() {
        log.info("Mail enabled={} host={} port={} username={}", settings.enabled(), smtpHost, smtpPort, mask(smtpUsername));
    }

    public String deliveryNote() {
        if (!settings.enabled()) {
            return "Mail is disabled";
        }
        if (smtpUsername == null || smtpUsername.isBlank() || mailSender.getIfAvailable() == null) {
            return "SMTP not configured";
        }
        return "Queued";
    }

    public Map<String, Object> diagnostics() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", settings.enabled());
        body.put("configured", deliveryNote().equals("Queued"));
        body.put("host", smtpHost);
        body.put("port", smtpPort);
        body.put("username", mask(smtpUsername));
        body.put("reason", deliveryNote().equals("Queued") ? "Ready" : deliveryNote());
        return body;
    }

    public List<Map<String, Object>> related(String type, String id) {
        return outboxRepository.findByRelatedTypeAndRelatedIdOrderByCreatedAtDesc(type, id).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("subject", row.getSubject());
            item.put("status", row.getStatus());
            item.put("error", row.getLastError());
            item.put("createdAt", row.getCreatedAt());
            return item;
        }).toList();
    }

    @Transactional
    public EmailOutbox enqueue(String to, String subject, String html, String template, String language,
                               String relatedType, String relatedId) {
        EmailOutbox row = new EmailOutbox();
        row.setToEmail(to);
        row.setSubject(subject);
        row.setBodyHtml(html.length() > 8000 ? html.substring(0, 8000) : html);
        row.setTemplateName(template);
        row.setLanguageCode(language);
        row.setRelatedType(relatedType);
        row.setRelatedId(relatedId);
        row.setStatus("QUEUED");
        row.setAttempts(0);
        outboxRepository.save(row);
        return row;
    }

    public String layout(String title, String body) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f5f7;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f5f7;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="520" cellpadding="0" cellspacing="0" style="width:100%%;max-width:520px;background:#ffffff;border-radius:22px;overflow:hidden;border:1px solid #e7e8ec;">
                        <tr><td style="height:8px;background:#a3283a;font-size:0;line-height:0;">&nbsp;</td></tr>
                        <tr><td align="center" style="padding:28px 24px 4px;">
                          <img src="cid:viking-logo" width="148" alt="Viking FC" style="display:block;width:148px;height:auto;border:0;outline:none;" />
                        </td></tr>
                        <tr><td align="center" style="padding:4px 32px 0;font-family:Segoe UI,Arial,sans-serif;">
                          <p style="margin:0;font-size:11px;letter-spacing:.24em;color:#a3283a;font-weight:700;">ONE CLUB · ONE FAMILY</p>
                          <h1 style="margin:12px 0 0;font-size:22px;line-height:1.35;color:#111111;font-weight:700;">%s</h1>
                        </td></tr>
                        <tr><td style="padding:20px 32px 28px;font-family:Segoe UI,Arial,sans-serif;color:#1c1c1c;font-size:15px;line-height:1.5;">%s</td></tr>
                        <tr><td align="center" style="padding:16px 24px 20px;background:#111111;font-family:Segoe UI,Arial,sans-serif;color:#f7f7f7;font-size:12px;line-height:1.6;">
                          Viking FC · Kigali, Rwanda<br/>ONE CLUB | ONE FAMILY | ONE MISSION
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(title, body);
    }

    public void dispatch(Long id) {
        Runnable job = () -> applicationContext.getBean(ClubMailService.class).deliver(id);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    @Async
    public void deliver(Long id) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            EmailOutbox row = outboxRepository.findById(id).orElse(null);
            if (row == null || "SENT".equals(row.getStatus())) {
                return;
            }
            row.setAttempts(attempt);
            try {
                sendOrLog(row);
                row.setStatus("SENT");
                row.setSentAt(Instant.now());
                row.setLastError(null);
                outboxRepository.save(row);
                return;
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                row.setLastError(message.length() > 500 ? message.substring(0, 500) : message);
                row.setStatus(attempt == 3 ? "FAILED" : "QUEUED");
                outboxRepository.save(row);
                log.warn("Email {} to {} failed (attempt {}): {}", id, row.getToEmail(), attempt, row.getLastError());
                if (attempt < 3) {
                    sleep(attempt);
                }
            }
        }
    }

    private void sendOrLog(EmailOutbox row) throws Exception {
        if (!settings.enabled() || smtpUsername == null || smtpUsername.isBlank() || mailSender.getIfAvailable() == null) {
            log.info("DEV email (not sent) to={} subject={} template={}", row.getToEmail(), row.getSubject(), row.getTemplateName());
            throw new IllegalStateException(deliveryNote());
        }
        String from = settings.fromAddress() == null || settings.fromAddress().isBlank() ? smtpUsername : settings.fromAddress();
        var message = mailSender.getObject().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(from, settings.fromName() == null ? "Viking FC" : settings.fromName());
        helper.setTo(row.getToEmail());
        helper.setSubject(row.getSubject());
        helper.setText(row.getBodyHtml(), true);
        ClassPathResource logo = new ClassPathResource("brand/logo.png");
        if (logo.exists()) {
            helper.addInline("viking-logo", logo);
        }
        mailSender.getObject().send(message);
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int at = value.indexOf('@');
        if (at < 2) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(1000L * (1L << (attempt - 1)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
