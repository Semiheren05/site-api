package com.semih.siteapi.stats;

import com.semih.siteapi.stats.dto.ViewStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Admin-only. Access is enforced in SecurityConfig (/api/admin/** requires ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/stats")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatsController {

    private final PageViewService pageViewService;

    public AdminStatsController(PageViewService pageViewService) {
        this.pageViewService = pageViewService;
    }

    @GetMapping("/views")
    @Operation(summary = "Views per day and most read posts (?from=2026-10-01&to=2026-10-31, default: last 30 days)")
    public ResponseEntity<ViewStatsResponse> views(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(pageViewService.stats(from, to));
    }
}
