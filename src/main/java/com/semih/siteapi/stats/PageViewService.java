package com.semih.siteapi.stats;

import com.semih.siteapi.common.exception.InvalidDateRangeException;
import com.semih.siteapi.post.PostService;
import com.semih.siteapi.stats.dto.DailyViews;
import com.semih.siteapi.stats.dto.PostViews;
import com.semih.siteapi.stats.dto.ViewStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Privacy-friendly page views: no cookies, no stored IP addresses, no tracking across days.
 * A visitor is counted at most once per post per day; bots and visitors who ask not to be
 * tracked (Do Not Track / Global Privacy Control) are not counted at all.
 */
@Service
public class PageViewService {

    private static final Logger log = LoggerFactory.getLogger(PageViewService.class);

    private static final int MAX_RANGE_DAYS = 366;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int TOP_POSTS = 10;
    private static final Pattern BOT = Pattern.compile(
            "bot|crawl|spider|slurp|preview|headless|curl|wget|python-requests|httpclient|okhttp",
            Pattern.CASE_INSENSITIVE);

    private final DailyPostViewRepository repository;
    private final PostService postService;
    private final StatsMapper mapper;
    private final ZoneId zone;
    private final Clock clock;
    private final DailySalt salt = new DailySalt();

    public PageViewService(DailyPostViewRepository repository, PostService postService, StatsMapper mapper,
                           StatsProperties properties, Clock clock) {
        this.repository = repository;
        this.postService = postService;
        this.mapper = mapper;
        this.zone = properties.timeZone();
        this.clock = clock;
    }

    /**
     * Counts a view of a published post (404 for anything else).
     *
     * @param optedOut true when the browser sent "DNT: 1" or "Sec-GPC: 1"
     */
    @Transactional
    public void recordView(String slug, String clientIp, String userAgent, boolean optedOut) {
        Long postId = postService.findPublishedId(slug);
        if (optedOut || looksLikeBot(userAgent)) {
            return;
        }
        LocalDate today = today();
        if (repository.markVisitor(postId, today, visitorHash(today, clientIp, userAgent)) == 1) {
            repository.increment(postId, today);
        }
    }

    /**
     * Views between two days, both included, in the configured time zone.
     * Defaults to the last 30 days; at most 366 days at once.
     */
    @Transactional(readOnly = true)
    public ViewStatsResponse stats(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : today();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new InvalidDateRangeException("The period can be at most " + MAX_RANGE_DAYS + " days");
        }

        Map<LocalDate, Long> byDay = repository.countByDay(start, end).stream()
                .collect(Collectors.toMap(DayCount::day, DayCount::views));
        List<DailyViews> daily = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            DayCount count = new DayCount(day, byDay.getOrDefault(day, 0L));
            daily.add(mapper.toDailyViews(count));
        }
        long total = daily.stream().mapToLong(DailyViews::views).sum();
        List<PostViews> top = repository.topPosts(start, end, PageRequest.of(0, TOP_POSTS)).stream()
                .map(mapper::toPostViews)
                .toList();
        return new ViewStatsResponse(start, end, total, daily, top);
    }

    /** Yesterday's visitor hashes are no longer needed for counting once per day. */
    @Transactional
    public int forgetPastVisitors() {
        int deleted = repository.deleteVisitorsBefore(today());
        log.info("Deleted past visitor hashes count={}", deleted);
        return deleted;
    }

    static boolean looksLikeBot(String userAgent) {
        return userAgent == null || userAgent.isBlank() || BOT.matcher(userAgent).find();
    }

    String visitorHash(LocalDate day, String clientIp, String userAgent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.forDay(day));
            digest.update((clientIp + "|" + userAgent).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }
}
