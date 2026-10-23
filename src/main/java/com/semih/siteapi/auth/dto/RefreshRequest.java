package com.semih.siteapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of /refresh and /logout. */
public record RefreshRequest(@NotBlank @Size(max = 100) String refreshToken) {
}
