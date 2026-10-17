package com.bearsemih.siteapi.security;

import com.bearsemih.siteapi.user.Role;

/** The identity carried inside a verified access token. */
public record TokenClaims(String email, Role role) {
}
