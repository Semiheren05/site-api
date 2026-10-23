package com.semih.siteapi.auth;

import com.semih.siteapi.common.exception.InvalidRefreshTokenException;
import com.semih.siteapi.security.JwtProperties;
import com.semih.siteapi.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens with rotation and reuse detection.
 *
 * <ul>
 *   <li>A token is 32 random bytes. Only its SHA-256 hash is stored. (Not BCrypt: that is for
 *       guessable passwords; a random 256-bit token cannot be guessed, and a plain hash can be
 *       looked up directly.)</li>
 *   <li>Every refresh revokes the used token and hands out a new one in the same family.</li>
 *   <li>A revoked token used again means it was copied: someone else may hold the family's
 *       newest token. The whole family is revoked, and that login has to sign in again.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final Duration lifetime;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties, Clock clock) {
        this.repository = repository;
        this.lifetime = properties.refreshExpiration();
        this.clock = clock;
    }

    /** Starts a new family: called at login and registration. */
    @Transactional
    public IssuedRefreshToken issue(User user) {
        return create(user, UUID.randomUUID(), clock.instant());
    }

    /**
     * Exchanges a valid token for a new one. {@code noRollbackFor}: when a reuse is detected the
     * family is revoked <em>and</em> the exception is thrown; without it, the exception would roll
     * the revocation back and the stolen token would keep working.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String value) {
        Instant now = clock.instant();
        RefreshToken current = repository.findWithUserByTokenHash(hash(value))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (current.isRevoked()) {
            throw reuseDetected(current, now);
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        if (repository.revokeIfActive(current.getId(), now) == 0) {
            // Another request used the same token a moment ago
            throw reuseDetected(current, now);
        }
        log.debug("Refresh token rotated userId={}", current.getUser().getId());
        return new Rotation(current.getUser(), create(current.getUser(), current.getFamilyId(), now));
    }

    /** Logout: ends the login the token belongs to. Unknown tokens are ignored (nothing to end). */
    @Transactional
    public void revokeFamily(String value) {
        repository.findByTokenHash(hash(value)).ifPresent(token -> {
            repository.revokeFamily(token.getFamilyId(), clock.instant());
            log.info("Logged out familyId={}", token.getFamilyId());
        });
    }

    /** Expired tokens are useless, even for reuse detection (they fail anyway). */
    @Transactional
    public int deleteExpired() {
        return repository.deleteExpired(clock.instant());
    }

    public Duration getLifetime() {
        return lifetime;
    }

    private IssuedRefreshToken create(User user, UUID familyId, Instant now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(lifetime);
        repository.save(new RefreshToken(user, hash(value), familyId, expiresAt));
        return new IssuedRefreshToken(value, expiresAt);
    }

    private InvalidRefreshTokenException reuseDetected(RefreshToken token, Instant now) {
        int revoked = repository.revokeFamily(token.getFamilyId(), now);
        log.warn("Refresh token reuse detected, login revoked userId={} familyId={} revokedTokens={}",
                token.getUser().getId(), token.getFamilyId(), revoked);
        return new InvalidRefreshTokenException();
    }

    static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Every Java runtime must support SHA-256
            throw new IllegalStateException(e);
        }
    }

    /** The user to sign a new access token for, and the refresh token that replaces the used one. */
    public record Rotation(User user, IssuedRefreshToken token) {
    }
}
