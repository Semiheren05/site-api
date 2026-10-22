package com.semih.siteapi.project.dto;

import com.semih.siteapi.common.Slugs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * @param slug         optional; built from the title when left out
 * @param technologies e.g. ["Java 21", "Spring Boot"]; stored comma-separated, so no commas inside
 * @param displayOrder smaller numbers come first; defaults to 0
 */
public record ProjectRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = Slugs.MAX_LENGTH)
        @Pattern(regexp = Slugs.PATTERN, message = "must contain only lowercase letters, digits and single hyphens")
        String slug,
        @NotBlank @Size(max = 500) String summary,
        String description,
        @Size(max = 20)
        List<@NotBlank @Size(max = 30) @Pattern(regexp = "[^,]+", message = "must not contain commas") String> technologies,
        @URL @Size(max = 255) String repoUrl,
        @URL @Size(max = 255) String liveUrl,
        boolean featured,
        @PositiveOrZero int displayOrder,
        boolean published) {
}
