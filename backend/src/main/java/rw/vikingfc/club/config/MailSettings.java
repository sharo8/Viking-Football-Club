package rw.vikingfc.club.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailSettings(String fromAddress, String fromName, String frontendBaseUrl, boolean enabled) {
    public boolean smtpConfigured() {
        return enabled && fromAddress != null && !fromAddress.isBlank();
    }
}
