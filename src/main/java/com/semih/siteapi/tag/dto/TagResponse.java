package com.semih.siteapi.tag.dto;

/** A tag in the public tag list, with how many published posts use it. Built by {@code TagMapper}. */
public record TagResponse(String name, String slug, long postCount) {
}
