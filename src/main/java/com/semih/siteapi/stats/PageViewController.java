package com.semih.siteapi.stats;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by the website when a post is opened. A separate POST (instead of counting inside
 * GET /api/posts/{slug}) keeps GET free of side effects: link previews, prefetching and caches
 * do not count as readers.
 */
@RestController
@Tag(name = "Blog")
public class PageViewController {

    private final PageViewService pageViewService;

    public PageViewController(PageViewService pageViewService) {
        this.pageViewService = pageViewService;
    }

    @PostMapping("/api/posts/{slug}/views")
    @Operation(summary = "Count a view of a published post (204). No cookies; honors DNT and Sec-GPC")
    public ResponseEntity<Void> record(@PathVariable String slug, HttpServletRequest request) {
        boolean optedOut = "1".equals(request.getHeader("DNT")) || "1".equals(request.getHeader("Sec-GPC"));
        pageViewService.recordView(slug, request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT), optedOut);
        return ResponseEntity.noContent().build();
    }
}
