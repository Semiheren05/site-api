package com.semih.siteapi.stats;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Shortly after midnight (in the stats time zone), deletes the previous days' visitor hashes. */
@Component
public class VisitorCleanup {

    private final PageViewService pageViewService;

    public VisitorCleanup(PageViewService pageViewService) {
        this.pageViewService = pageViewService;
    }

    @Scheduled(cron = "${app.stats.cleanup-cron:0 5 0 * * *}", zone = "${app.stats.time-zone}")
    public void forgetPastVisitors() {
        pageViewService.forgetPastVisitors();
    }
}
