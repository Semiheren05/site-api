package com.semih.siteapi.project;

import com.semih.siteapi.common.Slugs;
import com.semih.siteapi.common.exception.InvalidSlugException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.common.exception.SlugAlreadyUsedException;
import com.semih.siteapi.project.dto.ProjectRequest;
import com.semih.siteapi.project.dto.ProjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    /** Admin list: same order as the public page, unpublished projects included. */
    private static final Sort ADMIN_ORDER = Sort.by(
            Sort.Order.desc("featured"), Sort.Order.asc("displayOrder"), Sort.Order.asc("id"));

    private final ProjectRepository projectRepository;
    private final ProjectMapper mapper;

    public ProjectService(ProjectRepository projectRepository, ProjectMapper mapper) {
        this.projectRepository = projectRepository;
        this.mapper = mapper;
    }

    // --- Public ---

    @Transactional(readOnly = true)
    public List<ProjectResponse> listPublished() {
        return projectRepository.findByPublishedTrueOrderByFeaturedDescDisplayOrderAscIdAsc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getPublished(String slug) {
        return projectRepository.findBySlugAndPublishedTrue(slug)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public List<ProjectResponse> listAll() {
        return projectRepository.findAll(ADMIN_ORDER).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        String slug = resolveSlug(request);
        if (projectRepository.existsBySlug(slug)) {
            throw new SlugAlreadyUsedException(slug);
        }
        Project project = projectRepository.save(mapper.toEntity(request, slug));
        log.info("Project created projectId={} slug={}", project.getId(), slug);
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = find(id);
        String slug = resolveSlug(request);
        if (projectRepository.existsBySlugAndIdNot(slug, id)) {
            throw new SlugAlreadyUsedException(slug);
        }
        mapper.updateEntity(project, request, slug);
        // Flush so @PreUpdate runs now and the response shows the new updatedAt
        projectRepository.flush();
        return mapper.toResponse(project);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(find(id));
        log.info("Project deleted projectId={}", id);
    }

    private Project find(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
    }

    /** Business rule: an explicit slug wins, otherwise it is built from the title. */
    private static String resolveSlug(ProjectRequest request) {
        String slug = request.slug() == null || request.slug().isBlank()
                ? Slugs.from(request.title())
                : request.slug();
        if (slug.isEmpty()) {
            throw new InvalidSlugException();
        }
        return slug;
    }
}
