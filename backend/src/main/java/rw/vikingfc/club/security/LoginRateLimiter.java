package rw.vikingfc.club.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.config.AppProperties;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private final int maxPerMinute;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(AppProperties properties) {
        this.maxPerMinute = properties.security().loginRequestsPerMinute();
    }

    public void check(String ip) {
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(ip, (key, existing) -> {
            if (existing == null || existing.minute != minute) {
                return new Window(minute, 1);
            }
            existing.count++;
            return existing;
        });
        if (window.count > maxPerMinute) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again in a minute.");
        }
    }

    private static final class Window {
        private final long minute;
        private int count;

        private Window(long minute, int count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
