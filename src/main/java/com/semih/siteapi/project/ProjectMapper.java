package com.semih.siteapi.project;

import com.semih.siteapi.project.dto.ProjectRequest;
import com.semih.siteapi.project.dto.ProjectResponse;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.semih.siteapi.common.Texts.trimToNull;

/** Converts between portfolio DTOs and the {@link Project} entity. */
@Component
public class ProjectMapper {

    /** @param slug already resolved and checked by the service */
    public Project toEntity(ProjectRequest request, String slug) {
        return new Project(toDetails(request, slug));
    }

    public void updateEntity(Project project, ProjectRequest request, String slug) {
        project.update(toDetails(request, slug));
    }

    public ProjectResponse toResponse(Project project) {
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

    /** Trimmed values, blanks as null, technologies without duplicates. */
    private static ProjectDetails toDetails(ProjectRequest request, String slug) {
        List<String> technologies = request.technologies() == null
                ? List.of()
                : request.technologies().stream().map(String::trim).distinct().toList();
        return new ProjectDetails(
                slug,
                request.title().trim(),
                request.summary().trim(),
                trimToNull(request.description()),
                technologies,
                trimToNull(request.repoUrl()),
                trimToNull(request.liveUrl()),
                request.featured(),
                request.displayOrder(),
                request.published());
    }
}
