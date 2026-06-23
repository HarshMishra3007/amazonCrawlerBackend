package com.amazon.productintelligence.service;

import com.amazon.productintelligence.config.CrawlerProperties;
import com.amazon.productintelligence.exception.CrawlInProgressException;
import com.amazon.productintelligence.exception.ResourceNotFoundException;
import com.amazon.productintelligence.model.CompetitorLink;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.model.ProductType;
import com.amazon.productintelligence.repository.CompetitorLinkRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private final ProductRepository productRepository;
    private final CompetitorLinkRepository competitorLinkRepository;
    private final ProductCrawlExecutor productCrawlExecutor;
    private final CrawlerProperties crawlerProperties;

    private final AtomicBoolean crawling = new AtomicBoolean(false);

    @Async("taskExecutor")
    public void crawlAllAsync() {
        crawlAll();
    }

    @Async("taskExecutor")
    public void crawlProductAsync(Long productId) {
        crawlProduct(productId);
    }

    public void crawlAll() {
        if (!crawling.compareAndSet(false, true)) {
            throw new CrawlInProgressException("A crawl is already in progress");
        }

        try {
            List<Product> products = productRepository.findAll();
            log.info("Starting crawl for {} products", products.size());

            for (Product product : products) {
                productCrawlExecutor.execute(product.getId());
                sleepBetweenCrawls();
            }

            log.info("Completed crawl for {} products", products.size());
        } finally {
            crawling.set(false);
        }
    }

    public void crawlProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!crawling.compareAndSet(false, true)) {
            throw new CrawlInProgressException("A crawl is already in progress");
        }

        try {
            productCrawlExecutor.execute(productId);

            if (product.getType() == ProductType.OWN) {
                List<CompetitorLink> links = competitorLinkRepository.findByOwnProductIdWithCompetitor(productId);
                log.info("Crawling {} competitors for own product id={}", links.size(), productId);
                for (CompetitorLink link : links) {
                    sleepBetweenCrawls();
                    productCrawlExecutor.execute(link.getCompetitorProduct().getId());
                }
            }
        } finally {
            crawling.set(false);
        }
    }

    private void sleepBetweenCrawls() {
        try {
            Thread.sleep(crawlerProperties.getDelayMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
