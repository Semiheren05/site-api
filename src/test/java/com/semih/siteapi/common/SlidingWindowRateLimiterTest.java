package com.semih.siteapi.common;

import com.semih.siteapi.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlidingWindowRateLimiterTest {

    private final MovableClock clock = new MovableClock(Instant.parse("2026-10-22T10:00:00Z"));
    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, Duration.ofMinutes(10), clock);

    @Test
    void allowsUpToTheLimitThenRejects() {
        assertThat(limiter.tryAcquire("1.2.3.4")).isEmpty();
        assertThat(limiter.tryAcquire("1.2.3.4")).isEmpty();
        assertThat(limiter.tryAcquire("1.2.3.4")).isEmpty();

        assertThat(limiter.tryAcquire("1.2.3.4")).isPresent();
    }

    @Test
    void eachClientHasItsOwnLimit() {
        limiter.tryAcquire("1.1.1.1");
        limiter.tryAcquire("1.1.1.1");
        limiter.tryAcquire("1.1.1.1");

        assertThat(limiter.tryAcquire("2.2.2.2")).isEmpty();
    }

    @Test
    void retryAfterIsTheTimeUntilTheOldestRequestLeavesTheWindow() {
        limiter.tryAcquire("ip");
        clock.advance(Duration.ofMinutes(2));
        limiter.tryAcquire("ip");
        limiter.tryAcquire("ip");
        clock.advance(Duration.ofMinutes(1));

        // First request was at 10:00, now is 10:03: it leaves the window at 10:10
        assertThat(limiter.tryAcquire("ip")).contains(Duration.ofMinutes(7));
    }

    @Test
    void windowSlidesSoOldRequestsStopCounting() {
        limiter.tryAcquire("ip");
        clock.advance(Duration.ofMinutes(5));
        limiter.tryAcquire("ip");
        limiter.tryAcquire("ip");

        clock.advance(Duration.ofMinutes(5));

        // The 10:00 request is now exactly 10 minutes old and no longer counts
        assertThat(limiter.tryAcquire("ip")).isEmpty();
        assertThat(limiter.tryAcquire("ip")).isPresent();
    }

    @Test
    void rejectedRequestsDoNotExtendTheBlock() {
        limiter.tryAcquire("ip");
        limiter.tryAcquire("ip");
        limiter.tryAcquire("ip");
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("ip");
        }

        clock.advance(Duration.ofMinutes(10));

        assertThat(limiter.tryAcquire("ip")).isEmpty();
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(0, Duration.ofMinutes(1), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(1, Duration.ZERO, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retryAfterHeaderIsRoundedUpToWholeSeconds() {
        assertThat(new RateLimitExceededException(Duration.ofMillis(1500)).getRetryAfterSeconds()).isEqualTo(2);
        assertThat(new RateLimitExceededException(Duration.ofSeconds(420)).getRetryAfterSeconds()).isEqualTo(420);
        assertThat(new RateLimitExceededException(Duration.ZERO).getRetryAfterSeconds()).isEqualTo(1);
    }

    /** A clock the test can move forward, to check time-based rules without waiting. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
