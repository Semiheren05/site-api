package com.semih.siteapi.post.dto;

import com.semih.siteapi.common.Slugs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param slug optional; built from the title when left out
 * @param tags optional, e.g. ["Spring Boot", "JPA"]; new tags are created, existing ones reused.
 *             Left out or empty means the post has no tags (PUT replaces the whole post).
 */
public record PostRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = Slugs.MAX_LENGTH)
        @Pattern(regexp = Slugs.PATTERN, message = "must contain only lowercase letters, digits and single hyphens")
        String slug,
        @Size(max = 500) String summary,
        @NotBlank String content,
        @Size(max = 10)
        List<@NotBlank @Size(max = 30) String> tags) {
}
