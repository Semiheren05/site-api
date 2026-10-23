package com.semih.siteapi.tag.dto;

/** A tag as shown on a post: the name to display and the slug to link to (?tag=spring-boot). */
public record TagSummary(String name, String slug) {
}
