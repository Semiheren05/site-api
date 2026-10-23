package com.semih.siteapi.stats;

/** Query result: total views of one post in a period. */
public record PostCount(String slug, String title, Long views) {
}
