package com.semih.siteapi.post;

import com.semih.siteapi.common.Locations;
import com.semih.siteapi.post.dto.PostRequest;
import com.semih.siteapi.post.dto.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only blog management. Access is enforced in SecurityConfig (/api/admin/** requires ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/posts")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminPostController {

    private final PostService postService;

    public AdminPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Operation(summary = "List all posts including drafts, newest first")
    public ResponseEntity<Page<PostResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.listAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "A post by id, draft or published")
    public ResponseEntity<PostResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(postService.get(id));
    }

    @PostMapping
    @Operation(summary = "Create a draft post; the slug is built from the title if left out")
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest request) {
        PostResponse created = postService.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a post's title, slug, summary and content")
    public ResponseEntity<PostResponse> update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Make a post public")
    public ResponseEntity<PostResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(postService.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Move a post back to drafts")
    public ResponseEntity<PostResponse> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(postService.unpublish(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
