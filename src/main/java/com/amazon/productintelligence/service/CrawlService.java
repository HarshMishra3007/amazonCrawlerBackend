package com.amazon.productintelligence.service;

import com.amazon.productintelligence.config.CrawlerProperties;
import com.amazon.productintelligence.exception.CrawlInProgressException;
import com.amazon.productintelligence.exception.ResourceNotFoundException;
import com.amazon.productintelligence.model.CompetitorLink;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.model.ProductType;
import com.amazon.productintelligence.repository.CompetitorLinkRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class CrawlService {

    private final ProductRepository productRepository;
    private final CompetitorLinkRepository competitorLinkRepository;
    private final ProductCrawlExecutor productCrawlExecutor;
    private final CrawlerProperties crawlerProperties;
    private final CrawlService self;

    private final AtomicBoolean crawling = new AtomicBoolean(false);

    public CrawlService(
            ProductRepository productRepository,
            CompetitorLinkRepository competitorLinkRepository,
            ProductCrawlExecutor productCrawlExecutor,
            CrawlerProperties crawlerProperties,
            @Lazy CrawlService self) {
        this.productRepository = productRepository;
        this.competitorLinkRepository = competitorLinkRepository;
        this.productCrawlExecutor = productCrawlExecutor;
        this.crawlerProperties = crawlerProperties;
        this.self = self;
    }

    /**
     * Queues a product crawl. Marks targets PENDING in the database before returning so
     * admin UI and page refresh see the in-progress state immediately.
     */
    public void requestProductCrawl(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!crawling.compareAndSet(false, true)) {
            throw new CrawlInProgressException("A crawl is already in progress");
        }

        try {
            markPending(resolveTargetIds(product));
            self.runProductCrawlAsync(productId, product.getType());
        } catch (RuntimeException ex) {
            crawling.set(false);
            throw ex;
        }
    }

    public void requestFullCrawl() {
        if (!crawling.compareAndSet(false, true)) {
            throw new CrawlInProgressException("A crawl is already in progress");
        }

        try {
            List<Product> products = productRepository.findAll();
            markPending(products.stream().map(Product::getId).toList());
            self.runFullCrawlAsync();
        } catch (RuntimeException ex) {
            crawling.set(false);
            throw ex;
        }
    }

    public void crawlProductAsync(Long productId) {
        try {
            requestProductCrawl(productId);
        } catch (CrawlInProgressException ex) {
            log.warn("Crawl skipped for product id={}: {}", productId, ex.getMessage());
        }
    }

    public void crawlAllAsync() {
        try {
            requestFullCrawl();
        } catch (CrawlInProgressException ex) {
            log.warn("Full crawl skipped: {}", ex.getMessage());
        }
    }

    @Async("taskExecutor")
    public void runProductCrawlAsync(Long productId, ProductType type) {
        try {
            runProductCrawlWork(productId, type);
        } finally {
            crawling.set(false);
        }
    }

    @Async("taskExecutor")
    public void runFullCrawlAsync() {
        try {
            runFullCrawlWork();
        } finally {
            crawling.set(false);
        }
    }

    public void crawlAll() {
        if (!crawling.compareAndSet(false, true)) {
            throw new CrawlInProgressException("A crawl is already in progress");
        }

        try {
            runFullCrawlWork();
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
            runProductCrawlWork(productId, product.getType());
        } finally {
            crawling.set(false);
        }
    }

    private void runProductCrawlWork(Long productId, ProductType type) {
        productCrawlExecutor.execute(productId);

        if (type == ProductType.OWN) {
            List<CompetitorLink> links = competitorLinkRepository.findByOwnProductIdWithCompetitor(productId);
            log.info("Crawling {} competitors for own product id={}", links.size(), productId);
            for (CompetitorLink link : links) {
                sleepBetweenCrawls();
                productCrawlExecutor.execute(link.getCompetitorProduct().getId());
            }
        }
    }

    private void runFullCrawlWork() {
        List<Product> products = productRepository.findAll();
        log.info("Starting crawl for {} products", products.size());

        for (Product product : products) {
            productCrawlExecutor.execute(product.getId());
            sleepBetweenCrawls();
        }

        log.info("Completed crawl for {} products", products.size());
    }

    private List<Long> resolveTargetIds(Product product) {
        List<Long> ids = new ArrayList<>();
        ids.add(product.getId());
        if (product.getType() == ProductType.OWN) {
            competitorLinkRepository.findByOwnProductIdWithCompetitor(product.getId()).stream()
                    .map(link -> link.getCompetitorProduct().getId())
                    .forEach(ids::add);
        }
        return ids;
    }

    private void markPending(List<Long> productIds) {
        for (Long productId : productIds) {
            productCrawlExecutor.markPending(productId);
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
