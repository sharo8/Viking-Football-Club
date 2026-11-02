package rw.vikingfc.club;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import rw.vikingfc.club.config.AppProperties;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
public class VikingFcApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Kigali"));
        loadEnv(Path.of(".env"));
        loadEnv(Path.of("backend", ".env"));
        normalizeRenderDatabaseUrl();
        SpringApplication.run(VikingFcApplication.class, args);
    }

    /**
     * Render sets DATABASE_URL to postgres://user:password@host:port/db.
     * JDBC needs jdbc:postgresql:// and separate username and password.
     */
    private static void normalizeRenderDatabaseUrl() {
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            return;
        }
        String url = System.getenv("DATABASE_URL");
        if (url == null || url.isBlank()) {
            url = System.getProperty("DATABASE_URL");
        }
        if (url == null || url.isBlank() || url.startsWith("jdbc:")) {
            return;
        }
        if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) {
            return;
        }
        URI uri = URI.create(url);
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbc += "?" + uri.getQuery();
        }
        System.setProperty("spring.datasource.url", jdbc);
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return;
        }
        int colon = userInfo.indexOf(':');
        String username = URLDecoder.decode(colon < 0 ? userInfo : userInfo.substring(0, colon), StandardCharsets.UTF_8);
        String password = colon < 0 ? "" : URLDecoder.decode(userInfo.substring(colon + 1), StandardCharsets.UTF_8);
        if (System.getenv("DATABASE_USERNAME") == null) {
            System.setProperty("spring.datasource.username", username);
        }
        if (System.getenv("DATABASE_PASSWORD") == null) {
            System.setProperty("spring.datasource.password", password);
        }
    }

    private static void loadEnv(Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int split = trimmed.indexOf('=');
                String key = trimmed.substring(0, split).trim();
                String value = trimmed.substring(split + 1).trim();
                if (key.isEmpty() || System.getenv(key) != null || System.getProperty(key) != null) {
                    continue;
                }
                if ("MAIL_SMTP_PASSWORD".equals(key)) {
                    value = value.replace(" ", "");
                }
                System.setProperty(key, value);
            }
        } catch (Exception ignored) {
            // A missing local env file leaves the yaml defaults in place.
        }
    }
}
