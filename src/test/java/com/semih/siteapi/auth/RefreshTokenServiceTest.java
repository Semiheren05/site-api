package com.semih.siteapi.auth;

import com.semih.siteapi.common.exception.InvalidRefreshTokenException;
import com.semih.siteapi.security.JwtProperties;
import com.semih.siteapi.user.Role;
import com.semih.siteapi.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");
    private static final UUID FAMILY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private final User user = new User("semih@example.com", "hash", "Semih", Role.USER);

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("secret", Duration.ofMinutes(15), Duration.ofDays(30));
        service = new RefreshTokenService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void onlyTheHashIsStored() {
        IssuedRefreshToken issued = service.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(RefreshTokenService.hash(issued.value()))
                .doesNotContain(issued.value())
                .hasSize(64);
        assertThat(issued.value()).hasSize(43); // 32 random bytes, base64url without padding
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void rotationRevokesTheUsedTokenAndContinuesTheFamily() {
        RefreshToken current = token(NOW.plusSeconds(60));
        when(repository.findWithUserByTokenHash(RefreshTokenService.hash("valid"))).thenReturn(Optional.of(current));
        when(repository.revokeIfActive(7L, NOW)).thenReturn(1);

        RefreshTokenService.Rotation rotation = service.rotate("valid");

        assertThat(rotation.user()).isSameAs(user);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(FAMILY);
        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void reusingARevokedTokenRevokesTheWholeLogin() {
        RefreshToken used = token(NOW.plusSeconds(60));
        ReflectionTestUtils.setField(used, "revokedAt", NOW.minusSeconds(30));
        when(repository.findWithUserByTokenHash(any())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.rotate("copied"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository).revokeFamily(FAMILY, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void losingARaceForTheSameTokenCountsAsReuse() {
        when(repository.findWithUserByTokenHash(any())).thenReturn(Optional.of(token(NOW.plusSeconds(60))));
        when(repository.revokeIfActive(7L, NOW)).thenReturn(0);

        assertThatThrownBy(() -> service.rotate("twice"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository).revokeFamily(FAMILY, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void expiredAndUnknownTokensAreRejectedWithoutSideEffects() {
        when(repository.findWithUserByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(token(NOW)));
        when(repository.findWithUserByTokenHash(RefreshTokenService.hash("unknown"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("old")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate("unknown")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).revokeIfActive(anyLong(), any());
        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void theHashIsStableAndHexEncoded() {
        assertThat(RefreshTokenService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private RefreshToken token(Instant expiresAt) {
        RefreshToken token = new RefreshToken(user, "hash", FAMILY, expiresAt);
        ReflectionTestUtils.setField(token, "id", 7L);
        return token;
    }
}
