package com.semih.siteapi.auth;

import java.time.Instant;

/** A freshly created refresh token: the plain value goes to the client once and is never stored. */
public record IssuedRefreshToken(String value, Instant expiresAt) {
}
