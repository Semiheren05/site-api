package com.semih.siteapi.project.dto;

import com.semih.siteapi.project.Project;

import java.time.Instant;
import java.util.List;

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

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getSummary(),
                project.getDescription(),
                project.getTechnologies(),
                project.getRepoUrl(),
                project.getLiveUrl(),
                project.isFeatured(),
                project.getDisplayOrder(),
                project.isPublished(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
