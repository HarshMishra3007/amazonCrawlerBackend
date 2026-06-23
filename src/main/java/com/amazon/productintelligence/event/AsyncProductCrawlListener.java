package com.amazon.productintelligence.event;

import com.amazon.productintelligence.exception.CrawlInProgressException;
import com.amazon.productintelligence.service.CrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncProductCrawlListener {

    private final CrawlService crawlService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductAdded(ProductAddedEvent event) {
        log.info("Auto-crawling newly added own product id={}", event.ownProductId());
        runCrawl(event.ownProductId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompetitorLinked(CompetitorLinkedEvent event) {
        log.info("Auto-crawling own product id={} and all linked competitors after competitor link", event.ownProductId());
        runCrawl(event.ownProductId());
    }

    private void runCrawl(Long ownProductId) {
        try {
            crawlService.crawlProduct(ownProductId);
        } catch (CrawlInProgressException ex) {
            log.warn("Auto-crawl skipped for own product id={}: {}", ownProductId, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Auto-crawl failed for own product id={}: {}", ownProductId, ex.getMessage());
        }
    }
}
