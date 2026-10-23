package com.semih.siteapi.project.dto;

import java.time.Instant;
import java.util.List;

/** Built by {@code ProjectMapper}. */
public record ProjectResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String description,
        List<String> technologies,
        String repoUrl,
        String liveUrl,
        boolean featured,
        int displayOrder,
        boolean published,
        Instant createdAt,
        Instant updatedAt) {
}
