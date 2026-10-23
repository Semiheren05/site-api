package com.semih.siteapi.stats;

import com.semih.siteapi.stats.dto.DailyViews;
import com.semih.siteapi.stats.dto.PostViews;
import org.springframework.stereotype.Component;

@Component
public class StatsMapper {

    public DailyViews toDailyViews(DayCount count) {
        return new DailyViews(count.day(), count.views());
    }

    public PostViews toPostViews(PostCount count) {
        return new PostViews(count.slug(), count.title(), count.views());
    }
}
