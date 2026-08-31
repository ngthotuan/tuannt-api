package com.tuannt.api.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tuannt7 on 01/09/2026
 * <p>
 * Sliding-window limiter kept in memory. The contact endpoint is public and unauthenticated,
 * so it needs some abuse control, and a single Heroku dyno makes per-instance state adequate.
 * If this ever runs on more than one dyno the limit becomes per-dyno, not global.
 */
@Slf4j
@Component
public class RateLimiter {
    private static final int MAX_ENTRIES = 10_000;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /**
     * @return true when the caller is inside the limit and the hit has been recorded.
     */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        if (key == null || key.isBlank()) {
            return true;
        }
        // Cheap guard against unbounded growth from spoofed client addresses.
        if (hits.size() > MAX_ENTRIES) {
            log.warn("RateLimiter reached {} tracked keys, clearing", MAX_ENTRIES);
            hits.clear();
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
