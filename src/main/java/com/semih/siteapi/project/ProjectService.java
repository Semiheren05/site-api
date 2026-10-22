package com.semih.siteapi.project;

import com.semih.siteapi.common.Slugs;
import com.semih.siteapi.common.exception.InvalidSlugException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.common.exception.SlugAlreadyUsedException;
import com.semih.siteapi.project.dto.ProjectRequest;
import com.semih.siteapi.project.dto.ProjectResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    /** Admin list: same order as the public page, unpublished projects included. */
    private static final Sort ADMIN_ORDER = Sort.by(
            Sort.Order.desc("featured"), Sort.Order.asc("displayOrder"), Sort.Order.asc("id"));

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // --- Public ---

    @Transactional(readOnly = true)
    public List<ProjectResponse> listPublished() {
        return projectRepository.findByPublishedTrueOrderByFeaturedDescDisplayOrderAscIdAsc().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getPublished(String slug) {
        return projectRepository.findBySlugAndPublishedTrue(slug)
                .map(ProjectResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public List<ProjectResponse> listAll() {
        return projectRepository.findAll(ADMIN_ORDER).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return ProjectResponse.from(find(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        ProjectDetails details = toDetails(request);
        if (projectRepository.existsBySlug(details.slug())) {
            throw new SlugAlreadyUsedException(details.slug());
        }
        return ProjectResponse.from(projectRepository.save(new Project(details)));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = find(id);
        ProjectDetails details = toDetails(request);
        if (projectRepository.existsBySlugAndIdNot(details.slug(), id)) {
            throw new SlugAlreadyUsedException(details.slug());
        }
        project.update(details);
        // Flush so @PreUpdate runs now and the response shows the new updatedAt
        projectRepository.flush();
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(find(id));
    }

    private Project find(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
    }

    private static ProjectDetails toDetails(ProjectRequest request) {
        String slug = request.slug() == null || request.slug().isBlank()
                ? Slugs.from(request.title())
                : request.slug();
        if (slug.isEmpty()) {
            throw new InvalidSlugException();
        }
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

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
