package com.semih.siteapi.common.exception;

import java.time.Duration;

/** Answered with 429 Too Many Requests and a Retry-After header. */
public class RateLimitExceededException extends RuntimeException {

    private final Duration retryAfter;

    public RateLimitExceededException(Duration retryAfter) {
        super("Too many requests, please try again later");
        this.retryAfter = retryAfter;
    }

    /** Whole seconds, rounded up, at least 1: the format the Retry-After header expects. */
    public long getRetryAfterSeconds() {
        long seconds = retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
        return Math.max(1, seconds);
    }
}
