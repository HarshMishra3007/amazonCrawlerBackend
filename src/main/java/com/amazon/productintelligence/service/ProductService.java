package com.amazon.productintelligence.service;

import com.amazon.productintelligence.config.CacheConfig;
import com.amazon.productintelligence.dto.BulkImportItemResult;
import com.amazon.productintelligence.dto.BulkImportResponse;
import com.amazon.productintelligence.dto.CompetitorComparisonResponse;
import com.amazon.productintelligence.dto.CreateProductRequest;
import com.amazon.productintelligence.dto.PriceHistoryPoint;
import com.amazon.productintelligence.dto.ProductDetailBundleResponse;
import com.amazon.productintelligence.dto.ProductMapper;
import com.amazon.productintelligence.dto.ProductResponse;
import com.amazon.productintelligence.dto.ProductSummaryResponse;
import com.amazon.productintelligence.event.ProductAddedEvent;
import com.amazon.productintelligence.exception.ConflictException;
import com.amazon.productintelligence.exception.ResourceNotFoundException;
import com.amazon.productintelligence.model.CrawlSnapshot;
import com.amazon.productintelligence.model.CompetitorLink;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.model.ProductType;
import com.amazon.productintelligence.repository.CompetitorLinkRepository;
import com.amazon.productintelligence.repository.CrawlSnapshotRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CrawlSnapshotRepository crawlSnapshotRepository;
    private final CompetitorLinkRepository competitorLinkRepository;
    private final ProductDetailLoader productDetailLoader;
    private final ProductCacheService productCacheService;
    private final CompetitorProductCleanup competitorProductCleanup;
    private final ApplicationEventPublisher eventPublisher;
    private final CrawlService crawlService;
    private final Executor dbReadExecutor;

    public ProductService(
            ProductRepository productRepository,
            CrawlSnapshotRepository crawlSnapshotRepository,
            CompetitorLinkRepository competitorLinkRepository,
            ProductDetailLoader productDetailLoader,
            ProductCacheService productCacheService,
            CompetitorProductCleanup competitorProductCleanup,
            ApplicationEventPublisher eventPublisher,
            CrawlService crawlService,
            @Qualifier("dbReadExecutor") Executor dbReadExecutor) {
        this.productRepository = productRepository;
        this.crawlSnapshotRepository = crawlSnapshotRepository;
        this.competitorLinkRepository = competitorLinkRepository;
        this.productDetailLoader = productDetailLoader;
        this.productCacheService = productCacheService;
        this.competitorProductCleanup = competitorProductCleanup;
        this.eventPublisher = eventPublisher;
        this.crawlService = crawlService;
        this.dbReadExecutor = dbReadExecutor;
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCTS_LIST)
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> listOwnProducts() {
        return productRepository.findByType(ProductType.OWN).stream()
                .map(ProductMapper::toSummary)
                .toList();
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCT_DETAIL, key = "#id")
    public ProductDetailBundleResponse getProductDetail(Long id) {
        CompletableFuture<Product> productFuture = CompletableFuture.supplyAsync(
                () -> productDetailLoader.loadProduct(id), dbReadExecutor);
        CompletableFuture<List<PriceHistoryPoint>> historyFuture = CompletableFuture.supplyAsync(
                () -> productDetailLoader.loadHistory(id), dbReadExecutor);
        CompletableFuture<List<CompetitorLink>> linksFuture = CompletableFuture.supplyAsync(
                () -> productDetailLoader.loadCompetitorLinks(id), dbReadExecutor);

        Product product = productFuture.join();
        List<PriceHistoryPoint> history = historyFuture.join();
        List<CompetitorComparisonResponse> competitors = product.getType() == ProductType.OWN
                ? buildCompetitorComparisons(product, history, linksFuture.join())
                : List.of();

        return ProductDetailBundleResponse.builder()
                .product(ProductMapper.toResponse(product, history))
                .priceHistory(history)
                .competitors(competitors)
                .build();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductMapper.toResponse(getProductEntity(id));
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryPoint> getPriceHistory(Long id) {
        getProductEntity(id);
        return crawlSnapshotRepository.findByProductIdOrderByCrawledAtAsc(id).stream()
                .map(snapshot -> PriceHistoryPoint.builder()
                        .date(snapshot.getCrawledAt())
                        .price(snapshot.getPrice())
                        .build())
                .toList();
    }

    @Transactional
    public ProductResponse createOwnProduct(CreateProductRequest request) {
        if (productRepository.existsByAsin(request.getAsin())) {
            throw new ConflictException("Product with ASIN already exists: " + request.getAsin());
        }

        Product product = new Product();
        product.setAsin(request.getAsin());
        product.setType(ProductType.OWN);
        product = productRepository.save(product);
        productCacheService.evictAll();
        eventPublisher.publishEvent(new ProductAddedEvent(product.getId()));
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public BulkImportResponse createOwnProductsBulk(List<String> asins) {
        List<BulkImportItemResult> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (String asin : new LinkedHashSet<>(asins.stream().map(String::trim).map(String::toUpperCase).toList())) {
            if (productRepository.existsByAsin(asin)) {
                results.add(BulkImportItemResult.builder()
                        .asin(asin)
                        .success(false)
                        .message("ASIN already exists")
                        .build());
                failed++;
                continue;
            }

            Product product = new Product();
            product.setAsin(asin);
            product.setType(ProductType.OWN);
            product = productRepository.save(product);
            eventPublisher.publishEvent(new ProductAddedEvent(product.getId()));
            results.add(BulkImportItemResult.builder()
                    .asin(asin)
                    .success(true)
                    .message("Added")
                    .productId(product.getId())
                    .build());
            succeeded++;
        }

        if (succeeded > 0) {
            productCacheService.evictAll();
        }

        return BulkImportResponse.builder()
                .succeeded(succeeded)
                .failed(failed)
                .results(results)
                .build();
    }

    @Transactional
    public ProductResponse updateOwnProductAsin(Long id, String asin) {
        Product product = getProductEntity(id);
        if (product.getType() != ProductType.OWN) {
            throw new ConflictException("Only own products can be updated via this endpoint");
        }

        applyAsinChange(product, asin);
        productCacheService.evictAll();
        crawlService.crawlProductAsync(id);
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse updateCompetitorProductAsin(Long competitorProductId, String asin) {
        Product product = getProductEntity(competitorProductId);
        if (product.getType() != ProductType.COMPETITOR) {
            throw new ConflictException("Only competitor products can be updated via this endpoint");
        }

        applyAsinChange(product, asin);
        productCacheService.evictAll();
        return ProductMapper.toResponse(product);
    }

    void applyAsinChange(Product product, String newAsin) {
        String asin = newAsin.trim().toUpperCase();
        if (asin.equals(product.getAsin())) {
            return;
        }

        if (productRepository.existsByAsin(asin)) {
            throw new ConflictException("Product with ASIN already exists: " + asin);
        }

        product.setAsin(asin);
        resetCrawlCatalogData(product);
        productRepository.save(product);
    }

    void resetCrawlCatalogData(Product product) {
        product.setName(null);
        product.setDescription(null);
        product.setCurrentPrice(null);
        product.setSeller(null);
        product.setImages(new ArrayList<>());
        product.setLastCrawlStatus(null);
        product.setLastCrawlAt(null);
        product.setLastCrawlError(null);
        crawlSnapshotRepository.deleteByProductId(product.getId());
    }

    @Transactional
    public void deleteOwnProduct(Long id) {
        Product product = getProductEntity(id);
        if (product.getType() != ProductType.OWN) {
            throw new ConflictException("Only own products can be deleted via this endpoint");
        }

        List<Long> linkedCompetitorIds = competitorLinkRepository.findByOwnProductIdWithCompetitor(id).stream()
                .map(link -> link.getCompetitorProduct().getId())
                .distinct()
                .toList();

        competitorLinkRepository.deleteByOwnProductId(id);
        competitorProductCleanup.deleteIfOrphaned(linkedCompetitorIds);
        crawlSnapshotRepository.deleteByProductId(id);
        productRepository.delete(product);
        productCacheService.evictAll();
    }

    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public List<CrawlSnapshot> getSnapshots(Long productId) {
        return crawlSnapshotRepository.findByProductIdOrderByCrawledAtAsc(productId);
    }

    private List<CompetitorComparisonResponse> buildCompetitorComparisons(
            Product ownProduct,
            List<PriceHistoryPoint> ownPriceHistory,
            List<CompetitorLink> links) {
        if (links.isEmpty()) {
            return List.of();
        }

        List<Long> competitorIds = links.stream()
                .map(link -> link.getCompetitorProduct().getId())
                .toList();
        Map<Long, List<PriceHistoryPoint>> historyByProductId = ProductMapper.groupPriceHistoryByProductId(
                crawlSnapshotRepository.findByProductIdInOrderByCrawledAtAsc(competitorIds));

        return links.stream()
                .map(link -> ProductMapper.toCompetitorComparison(
                        ownProduct,
                        ownPriceHistory,
                        link,
                        historyByProductId.getOrDefault(link.getCompetitorProduct().getId(), List.of())))
                .toList();
    }
}
