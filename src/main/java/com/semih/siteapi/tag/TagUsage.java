package com.semih.siteapi.tag;

/** Query result of {@link TagRepository#findPublishedUsage()}: one row per tag, already counted. */
public record TagUsage(String name, String slug, Long postCount) {
}
