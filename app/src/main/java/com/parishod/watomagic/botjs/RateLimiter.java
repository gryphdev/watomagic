package com.parishod.watomagic.botjs;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rate limiter sencillo (ventana fija) para limitar ejecuciones de bots.
 */
public class RateLimiter {

    private final int maxExecutions;
    private final long windowMs;
    private final Deque<Long> executionTimes = new ArrayDeque<>();

    public RateLimiter(int maxExecutions, long windowMs) {
        this.maxExecutions = maxExecutions;
        this.windowMs = windowMs;
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        prune(now);

        if (executionTimes.size() >= maxExecutions) {
            return false;
        }

        executionTimes.addLast(now);
        return true;
    }

    private void prune(long now) {
        while (!executionTimes.isEmpty() && executionTimes.peekFirst() < now - windowMs) {
            executionTimes.removeFirst();
        }
    }
}
