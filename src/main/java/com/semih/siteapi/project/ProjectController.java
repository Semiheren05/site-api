package com.semih.siteapi.project;

import com.semih.siteapi.project.dto.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public, read-only portfolio API. Only published projects are visible here. */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Portfolio", description = "Published portfolio projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "Published projects, featured first, then by display order")
    public List<ProjectResponse> list() {
        return projectService.listPublished();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "A published project")
    public ProjectResponse get(@PathVariable String slug) {
        return projectService.getPublished(slug);
    }
}
