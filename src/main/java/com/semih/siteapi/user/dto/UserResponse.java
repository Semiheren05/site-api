package com.semih.siteapi.user.dto;

import com.semih.siteapi.user.Role;

import java.time.Instant;

/** Public view of a user. Never exposes the password hash. Built by {@code UserMapper}. */
public record UserResponse(Long id, String email, String displayName, Role role, Instant createdAt) {
}
