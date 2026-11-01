package rw.vikingfc.club.i18n;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A small per-user limit so the translation button cannot be hammered. */
public class TranslateRateLimiter {

    private final int limit;
    private final long windowMillis;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public TranslateRateLimiter(int limit, long windowMillis) {
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    public boolean allow(String key, Instant now) {
        long at = now.toEpochMilli();
        Deque<Long> deque = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && at - deque.peekFirst() > windowMillis) {
                deque.removeFirst();
            }
            if (deque.size() >= limit) {
                return false;
            }
            deque.addLast(at);
            return true;
        }
    }
}
