package rw.vikingfc.club.i18n;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslateRateLimiterTest {

    @Test
    void blocksOnceTheWindowIsFull() {
        TranslateRateLimiter limiter = new TranslateRateLimiter(3, 60_000);
        Instant now = Instant.parse("2026-10-31T12:00:00Z");
        assertTrue(limiter.allow("president", now));
        assertTrue(limiter.allow("president", now));
        assertTrue(limiter.allow("president", now));
        assertFalse(limiter.allow("president", now.plusSeconds(1)));
        assertTrue(limiter.allow("president", now.plusSeconds(61)));
    }
}
