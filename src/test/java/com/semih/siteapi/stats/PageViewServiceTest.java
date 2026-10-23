package com.semih.siteapi.stats;

import com.semih.siteapi.common.exception.InvalidDateRangeException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.post.PostService;
import com.semih.siteapi.stats.dto.ViewStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageViewServiceTest {

    // 22:30 UTC on the 23rd is already the 24th in Istanbul (UTC+3)
    private static final Instant NOW = Instant.parse("2026-10-23T22:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);
    private static final String BROWSER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/131.0";

    @Mock
    private DailyPostViewRepository repository;
    @Mock
    private PostService postService;

    private PageViewService service;

    @BeforeEach
    void setUp() {
        service = new PageViewService(repository, postService, new StatsMapper(),
                new StatsProperties(ZoneId.of("Europe/Istanbul")), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aVisitorIsCountedOncePerDayInTheConfiguredTimeZone() {
        when(postService.findPublishedId("yazi")).thenReturn(7L);
        when(repository.markVisitor(eq(7L), eq(TODAY), anyString())).thenReturn(1, 0);

        service.recordView("yazi", "203.0.113.5", BROWSER, false);
        service.recordView("yazi", "203.0.113.5", BROWSER, false);

        verify(repository, times(1)).increment(7L, TODAY);
    }

    @Test
    void botsAndVisitorsWhoOptedOutAreNotCounted() {
        when(postService.findPublishedId("yazi")).thenReturn(7L);

        service.recordView("yazi", "203.0.113.5", "Mozilla/5.0 (compatible; Googlebot/2.1)", false);
        service.recordView("yazi", "203.0.113.5", null, false);
        service.recordView("yazi", "203.0.113.5", BROWSER, true);

        verify(repository, never()).markVisitor(anyLong(), any(), anyString());
        verify(repository, never()).increment(anyLong(), any());
    }

    @Test
    void draftsAndUnknownPostsAreNotFound() {
        when(postService.findPublishedId("taslak")).thenThrow(new ResourceNotFoundException("Post not found"));

        assertThatThrownBy(() -> service.recordView("taslak", "203.0.113.5", BROWSER, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void theVisitorHashHidesTheIpAndChangesEveryDay() {
        String today = service.visitorHash(TODAY, "203.0.113.5", BROWSER);

        assertThat(today).hasSize(64).doesNotContain("203.0.113.5");
        assertThat(service.visitorHash(TODAY, "203.0.113.5", BROWSER)).isEqualTo(today);
        assertThat(service.visitorHash(TODAY, "203.0.113.6", BROWSER)).isNotEqualTo(today);
        assertThat(service.visitorHash(TODAY.plusDays(1), "203.0.113.5", BROWSER)).isNotEqualTo(today);
    }

    @Test
    void daysWithoutViewsAreFilledWithZero() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);
        when(repository.countByDay(from, to)).thenReturn(List.of(
                new DayCount(LocalDate.of(2026, 10, 2), 4L),
                new DayCount(LocalDate.of(2026, 10, 5), 1L)));
        when(repository.topPosts(eq(from), eq(to), any())).thenReturn(List.of(new PostCount("yazi", "Yazı", 5L)));

        ViewStatsResponse stats = service.stats(from, to);

        assertThat(stats.daily()).extracting(daily -> daily.views()).containsExactly(0L, 4L, 0L, 0L, 1L);
        assertThat(stats.totalViews()).isEqualTo(5);
        assertThat(stats.topPosts()).singleElement().satisfies(post -> assertThat(post.views()).isEqualTo(5));
    }

    @Test
    void theDefaultPeriodIsTheLast30DaysUntilToday() {
        ViewStatsResponse stats = service.stats(null, null);

        assertThat(stats.to()).isEqualTo(TODAY);
        assertThat(stats.from()).isEqualTo(TODAY.minusDays(29));
        assertThat(stats.daily()).hasSize(30);
    }

    @Test
    void invalidPeriodsAreRejected() {
        assertThatThrownBy(() -> service.stats(TODAY, TODAY.minusDays(1)))
                .isInstanceOf(InvalidDateRangeException.class);
        assertThatThrownBy(() -> service.stats(TODAY.minusDays(366), TODAY))
                .isInstanceOf(InvalidDateRangeException.class);
    }
}
