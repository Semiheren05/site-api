package com.semih.siteapi.post.dto;

import com.semih.siteapi.common.Slugs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param slug optional; built from the title when left out
 */
public record PostRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = Slugs.MAX_LENGTH)
        @Pattern(regexp = Slugs.PATTERN, message = "must contain only lowercase letters, digits and single hyphens")
        String slug,
        @Size(max = 500) String summary,
        @NotBlank String content) {
}
