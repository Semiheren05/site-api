package com.semih.siteapi.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows at most {@code maxRequests} per key (e.g. a client IP) in any rolling
 * {@code window}. "3 per 10 minutes" means: when a 4th request comes, look back
 * 10 minutes; if 3 requests are already there, reject it.
 *
 * <p>In memory, so limits reset on restart and are per instance. That is enough for
 * one server; several instances would need a shared store such as Redis.
 */
public class SlidingWindowRateLimiter {

    /** Above this many keys, keys with no recent requests are dropped so memory stays bounded. */
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> requests = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, Duration window, Clock clock) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive: " + maxRequests);
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    /**
     * Records the request if it is allowed.
     *
     * @return empty if allowed; otherwise how long until the next request would be allowed
     */
    public Optional<Duration> tryAcquire(String key) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(window);
        if (requests.size() > MAX_TRACKED_KEYS) {
            evictIdleKeys(windowStart);
        }

        Deque<Instant> timestamps = requests.computeIfAbsent(key, k -> new ArrayDeque<>());
        // Two requests from the same client can arrive at the same time; guard this client's list
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                // The oldest request leaves the window first
                return Optional.of(Duration.between(windowStart, timestamps.peekFirst()));
            }
            timestamps.addLast(now);
            return Optional.empty();
        }
    }

    private void evictIdleKeys(Instant windowStart) {
        requests.entrySet().removeIf(entry -> {
            Deque<Instant> timestamps = entry.getValue();
            synchronized (timestamps) {
                return timestamps.isEmpty() || !timestamps.peekLast().isAfter(windowStart);
            }
        });
    }
}
