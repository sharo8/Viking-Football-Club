package rw.vikingfc.club;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import rw.vikingfc.club.config.AppProperties;

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
        SpringApplication.run(VikingFcApplication.class, args);
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
