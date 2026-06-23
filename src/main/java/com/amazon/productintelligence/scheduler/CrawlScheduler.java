package com.amazon.productintelligence.scheduler;

import com.amazon.productintelligence.service.CrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlScheduler {

    private final CrawlService crawlService;

    @Scheduled(cron = "${crawler.schedule:0 0 */6 * * *}")
    public void scheduledCrawl() {
        log.info("Scheduled crawl triggered");
        try {
            crawlService.crawlAll();
        } catch (Exception ex) {
            log.error("Scheduled crawl failed: {}", ex.getMessage(), ex);
        }
    }
}
