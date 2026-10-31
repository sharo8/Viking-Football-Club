package rw.vikingfc.club.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Uploads uploads,
        Security security,
        boolean bootstrapDerivedData
) {
    public record Jwt(String secret, int accessMinutes, int refreshDays) {
    }

    public record Cors(String origins) {
        public List<String> originList() {
            return Arrays.stream(origins.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .toList();
        }
    }

    public record Uploads(String dir) {
    }

    public record Security(int maxFailedAttempts, int lockMinutes, int loginRequestsPerMinute) {
    }
}
