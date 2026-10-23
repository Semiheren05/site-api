package com.semih.siteapi.auth.dto;

/**
 * @param accessToken      short-lived JWT for the Authorization header
 * @param expiresIn        seconds until the access token expires
 * @param refreshToken     long-lived, single-use token for /api/auth/refresh; keep it out of reach of
 *                         page scripts where possible, it is a login on its own
 * @param refreshExpiresIn seconds until the refresh token expires
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn,
                           String refreshToken, long refreshExpiresIn) {

    public static AuthResponse bearer(String accessToken, long expiresIn, String refreshToken, long refreshExpiresIn) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, refreshToken, refreshExpiresIn);
    }
}
