package com.semih.siteapi.project;

import java.util.List;

/**
 * Cleaned-up values for creating or updating a {@link Project}: slug resolved,
 * text trimmed, blanks turned into nulls. Keeps the entity constructor short.
 */
record ProjectDetails(
        String slug,
        String title,
        String summary,
        String description,
        List<String> technologies,
        String repoUrl,
        String liveUrl,
        boolean featured,
        int displayOrder,
        boolean published) {
}
