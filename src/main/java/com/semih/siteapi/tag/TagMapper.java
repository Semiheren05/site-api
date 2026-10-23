package com.semih.siteapi.tag;

import com.semih.siteapi.tag.dto.TagResponse;
import com.semih.siteapi.tag.dto.TagSummary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Component
public class TagMapper {

    public TagResponse toResponse(TagUsage usage) {
        return new TagResponse(usage.name(), usage.slug(), usage.postCount());
    }

    /** Sorted by slug, so a post's tags always come out in the same order. */
    public List<TagSummary> toSummaries(Collection<Tag> tags) {
        return tags.stream()
                .sorted(Comparator.comparing(Tag::getSlug))
                .map(tag -> new TagSummary(tag.getName(), tag.getSlug()))
                .toList();
    }
}
