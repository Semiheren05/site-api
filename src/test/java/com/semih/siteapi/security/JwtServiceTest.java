package com.semih.siteapi.security;

import com.semih.siteapi.user.Role;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = base64("test-secret-that-is-at-least-32-bytes-long");
    private static final String OTHER_SECRET = base64("another-secret-that-is-also-32-bytes-or-more");
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void generatedTokenCanBeParsedBack() {
        JwtService service = serviceAt(NOW, SECRET);

        String token = service.generateToken("semih@example.com", Role.ADMIN);

        assertThat(service.parse(token)).contains(new TokenClaims("semih@example.com", Role.ADMIN));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceAt(NOW, SECRET).generateToken("semih@example.com", Role.USER);

        JwtService twoHoursLater = serviceAt(NOW.plus(Duration.ofHours(2)), SECRET);

        assertThat(twoHoursLater.parse(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = serviceAt(NOW, OTHER_SECRET).generateToken("semih@example.com", Role.ADMIN);

        assertThat(serviceAt(NOW, SECRET).parse(forged)).isEmpty();
    }

    @Test
    void malformedTokensAreRejected() {
        JwtService service = serviceAt(NOW, SECRET);

        assertThat(service.parse("not-a-jwt")).isEmpty();
        assertThat(service.parse("")).isEmpty();
    }

    private static JwtService serviceAt(Instant instant, String secret) {
        return new JwtService(
                new JwtProperties(secret, Duration.ofHours(1), Duration.ofDays(30)),
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
