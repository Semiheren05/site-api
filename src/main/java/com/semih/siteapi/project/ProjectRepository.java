package com.semih.siteapi.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** The portfolio page order: featured projects first, then by display order. */
    List<Project> findByPublishedTrueOrderByFeaturedDescDisplayOrderAscIdAsc();

    Optional<Project> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
