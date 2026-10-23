package com.semih.siteapi.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Deletes expired refresh tokens every night, so the table does not grow forever. */
@Component
public class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanup(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "${app.jwt.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    public void deleteExpired() {
        int deleted = refreshTokenService.deleteExpired();
        log.info("Deleted expired refresh tokens count={}", deleted);
    }
}
