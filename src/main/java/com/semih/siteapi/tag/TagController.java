package com.semih.siteapi.tag;

import com.semih.siteapi.tag.dto.TagResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public tag list, e.g. for a tag cloud on the blog. */
@RestController
@RequestMapping("/api/tags")
// Written out in full: in this package "Tag" is our entity
@io.swagger.v3.oas.annotations.tags.Tag(name = "Blog")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "Tags used by published posts, most used first")
    public ResponseEntity<List<TagResponse>> list() {
        return ResponseEntity.ok(tagService.listUsed());
    }
}
