package com.semih.siteapi.post;

import com.semih.siteapi.post.dto.PostResponse;
import com.semih.siteapi.post.dto.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only blog API. Only published posts are visible here. */
@RestController
@RequestMapping("/api/posts")
@Tag(name = "Blog", description = "Published blog posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Constraints on the parameters are checked by Spring MVC itself (method validation);
     * a too short search answers 400 in the usual problem-detail shape.
     */
    @GetMapping
    @Operation(summary = "Published posts, newest first; filter with ?tag=spring-boot, search with ?q=jwt "
            + "(paginated: ?page=0&size=10)")
    public ResponseEntity<Page<PostSummaryResponse>> list(
            @RequestParam(required = false) @Size(max = 40) String tag,
            @RequestParam(required = false) @Size(min = 2, max = 100) String q,
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.listPublished(tag, q, pageable));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "A published post with its full content")
    public ResponseEntity<PostResponse> get(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getPublished(slug));
    }
}
