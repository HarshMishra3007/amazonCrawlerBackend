package com.amazon.productintelligence.service;

import com.amazon.productintelligence.crawler.AmazonCrawler;
import com.amazon.productintelligence.crawler.CrawlException;
import com.amazon.productintelligence.crawler.CrawlResult;
import com.amazon.productintelligence.dto.ProductMapper;
import com.amazon.productintelligence.model.CrawlSnapshot;
import com.amazon.productintelligence.model.CrawlStatus;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.repository.CrawlSnapshotRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCrawlExecutor {

    private final ProductRepository productRepository;
    private final CrawlSnapshotRepository crawlSnapshotRepository;
    private final AmazonCrawler amazonCrawler;
    private final ProductCacheService productCacheService;

    @Transactional
    public void execute(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));

        product.setLastCrawlStatus(CrawlStatus.PENDING);
        product.setLastCrawlAt(Instant.now());
        productRepository.save(product);

        try {
            CrawlResult result = amazonCrawler.crawl(product.getAsin());
            persistSuccess(product, result);
        } catch (CrawlException ex) {
            persistFailure(product, ex.getReasonCode() + ": " + ex.getMessage());
        } catch (Exception ex) {
            persistFailure(product, ex.getMessage());
        }
    }

    private void persistSuccess(Product product, CrawlResult result) {
        ProductMapper.applyCrawlSuccess(product, result);
        product.setLastCrawlAt(Instant.now());
        productRepository.save(product);

        CrawlSnapshot snapshot = new CrawlSnapshot();
        snapshot.setProduct(product);
        snapshot.setPrice(result.getPrice());
        snapshot.setCurrency(result.getCurrency());
        snapshot.setSeller(result.getSeller());
        snapshot.setImages(result.getImages());
        snapshot.setCrawledAt(Instant.now());
        crawlSnapshotRepository.save(snapshot);
        productCacheService.evictAll();
    }

    private void persistFailure(Product product, String errorMessage) {
        product.setLastCrawlStatus(CrawlStatus.FAILED);
        product.setLastCrawlError(errorMessage);
        product.setLastCrawlAt(Instant.now());
        productRepository.save(product);
        productCacheService.evictAll();
        log.warn("Persisted crawl failure for ASIN={} error={}", product.getAsin(), errorMessage);
    }
}
