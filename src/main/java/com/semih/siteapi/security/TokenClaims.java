package com.semih.siteapi.security;

import com.semih.siteapi.user.Role;

/** The identity carried inside a verified access token. */
public record TokenClaims(String email, Role role) {
}
