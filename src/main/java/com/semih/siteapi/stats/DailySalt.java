package com.semih.siteapi.stats;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * A random value per day, kept only in memory. Hashing "salt + IP + browser" gives a visitor id
 * that works for one day and then becomes meaningless: the old salt is gone, so nobody (not even
 * with database access) can turn the hashes back into IP addresses or follow a visitor across days.
 * A restart also forgets the salt; at worst a visitor is counted twice that day.
 */
class DailySalt {

    private static final SecureRandom RANDOM = new SecureRandom();

    private LocalDate day;
    private byte[] value;

    synchronized byte[] forDay(LocalDate today) {
        if (!today.equals(day)) {
            value = new byte[16];
            RANDOM.nextBytes(value);
            day = today;
        }
        return value.clone();
    }
}
