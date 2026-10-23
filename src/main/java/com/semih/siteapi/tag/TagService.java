package com.semih.siteapi.tag;

import com.semih.siteapi.common.Slugs;
import com.semih.siteapi.common.exception.InvalidTagException;
import com.semih.siteapi.tag.dto.TagResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;
    private final TagMapper mapper;

    public TagService(TagRepository tagRepository, TagMapper mapper) {
        this.tagRepository = tagRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listUsed() {
        return tagRepository.findPublishedUsage().stream().map(mapper::toResponse).toList();
    }

    /**
     * For PostService: turns the names typed in the editor into tag entities. Existing tags are
     * reused (looked up in one query), new ones are created. Names that differ only in case or
     * Turkish letters ("Spring Boot", "spring boot") are the same tag; the first spelling wins.
     *
     * @param names may be null, meaning "no tags"
     */
    @Transactional
    public Set<Tag> resolve(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        Map<String, String> nameBySlug = new LinkedHashMap<>();
        for (String raw : names) {
            String name = raw.trim();
            String slug = Slugs.from(name);
            if (slug.isEmpty()) {
                throw new InvalidTagException(name);
            }
            nameBySlug.putIfAbsent(slug, name);
        }

        Map<String, Tag> existing = tagRepository.findBySlugIn(nameBySlug.keySet()).stream()
                .collect(Collectors.toMap(Tag::getSlug, Function.identity()));
        Set<Tag> result = new HashSet<>(existing.values());
        nameBySlug.forEach((slug, name) -> {
            if (!existing.containsKey(slug)) {
                result.add(tagRepository.save(new Tag(name, slug)));
                log.info("Tag created slug={}", slug);
            }
        });
        return result;
    }
}
