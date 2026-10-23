package com.semih.siteapi.stats;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DailyPostViewRepository extends JpaRepository<DailyPostView, DailyPostViewId> {

    /**
     * Remembers that this visitor read the post today. Returns 1 the first time, 0 after that:
     * the primary key and ON CONFLICT DO NOTHING make it a single atomic statement, so two
     * simultaneous requests from the same visitor cannot both count.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO post_view_visitors (post_id, viewed_on, visitor_hash)
            VALUES (:postId, :day, :visitorHash)
            ON CONFLICT DO NOTHING""")
    int markVisitor(Long postId, LocalDate day, String visitorHash);

    /** Adds one view, creating the day's row if needed (an "upsert"), without a read first. */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO post_daily_views (post_id, viewed_on, views)
            VALUES (:postId, :day, 1)
            ON CONFLICT (post_id, viewed_on) DO UPDATE SET views = post_daily_views.views + 1""")
    int increment(Long postId, LocalDate day);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM post_view_visitors WHERE viewed_on < :today")
    int deleteVisitorsBefore(LocalDate today);

    @Query("""
            select new com.semih.siteapi.stats.DayCount(v.id.viewedOn, sum(v.views))
            from DailyPostView v
            where v.id.viewedOn between :from and :to
            group by v.id.viewedOn
            order by v.id.viewedOn""")
    List<DayCount> countByDay(LocalDate from, LocalDate to);

    /** Most read posts of the period; the page size sets how many. */
    @Query("""
            select new com.semih.siteapi.stats.PostCount(p.slug, p.title, sum(v.views))
            from DailyPostView v join Post p on p.id = v.id.postId
            where v.id.viewedOn between :from and :to
            group by p.id, p.slug, p.title
            order by sum(v.views) desc, p.id""")
    List<PostCount> topPosts(LocalDate from, LocalDate to, Pageable pageable);
}
