package com.amazon.productintelligence.service;

import com.amazon.productintelligence.dto.AdminProductCatalogResponse;
import com.amazon.productintelligence.dto.BulkImportItemResult;
import com.amazon.productintelligence.dto.BulkImportResponse;
import com.amazon.productintelligence.dto.CompetitorComparisonResponse;
import com.amazon.productintelligence.dto.CreateCompetitorRequest;
import com.amazon.productintelligence.dto.ProductMapper;
import com.amazon.productintelligence.dto.ProductResponse;
import com.amazon.productintelligence.exception.ConflictException;
import com.amazon.productintelligence.exception.ResourceNotFoundException;
import com.amazon.productintelligence.event.CompetitorLinkedEvent;
import com.amazon.productintelligence.model.CompetitorLink;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.model.ProductType;
import com.amazon.productintelligence.repository.CompetitorLinkRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitorService {

    private final ProductRepository productRepository;
    private final CompetitorLinkRepository competitorLinkRepository;
    private final ProductService productService;
    private final ProductCacheService productCacheService;
    private final CompetitorProductCleanup competitorProductCleanup;
    private final ApplicationEventPublisher eventPublisher;
    private final CrawlService crawlService;

    @Transactional(readOnly = true)
    public List<CompetitorComparisonResponse> getCompetitorsForOwnProduct(Long ownProductId) {
        Product ownProduct = productService.getProductEntity(ownProductId);
        if (ownProduct.getType() != ProductType.OWN) {
            throw new ConflictException("Competitors can only be listed for own products");
        }

        return competitorLinkRepository.findByOwnProductIdWithCompetitor(ownProductId).stream()
                .map(link -> ProductMapper.toCompetitorComparison(ownProduct, List.of(), link, List.of()))
                .toList();
    }

    @Transactional
    public ProductResponse createCompetitor(CreateCompetitorRequest request) {
        Product ownProduct = productService.getProductEntity(request.getOwnProductId());
        if (ownProduct.getType() != ProductType.OWN) {
            throw new ConflictException("Competitors must be linked to an own product");
        }

        Product competitor = linkCompetitor(ownProduct, request.getAsin());
        productCacheService.evictAll();
        eventPublisher.publishEvent(new CompetitorLinkedEvent(ownProduct.getId()));

        return ProductMapper.toResponse(competitor);
    }

    @Transactional
    public BulkImportResponse createCompetitorsBulk(Long ownProductId, List<String> asins) {
        Product ownProduct = productService.getProductEntity(ownProductId);
        if (ownProduct.getType() != ProductType.OWN) {
            throw new ConflictException("Competitors must be linked to an own product");
        }

        List<BulkImportItemResult> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (String asin : new LinkedHashSet<>(asins.stream().map(String::trim).map(String::toUpperCase).toList())) {
            try {
                Product competitor = linkCompetitor(ownProduct, asin);
                results.add(BulkImportItemResult.builder()
                        .asin(asin)
                        .success(true)
                        .message("Linked")
                        .productId(competitor.getId())
                        .build());
                succeeded++;
            } catch (ConflictException ex) {
                results.add(BulkImportItemResult.builder()
                        .asin(asin)
                        .success(false)
                        .message(ex.getMessage())
                        .build());
                failed++;
            }
        }

        if (succeeded > 0) {
            productCacheService.evictAll();
            eventPublisher.publishEvent(new CompetitorLinkedEvent(ownProduct.getId()));
        }

        return BulkImportResponse.builder()
                .succeeded(succeeded)
                .failed(failed)
                .results(results)
                .build();
    }

    private Product linkCompetitor(Product ownProduct, String asin) {
        Product competitor = productRepository.findByAsin(asin)
                .orElseGet(() -> {
                    Product newProduct = new Product();
                    newProduct.setAsin(asin);
                    newProduct.setType(ProductType.COMPETITOR);
                    return productRepository.save(newProduct);
                });

        if (competitor.getType() != ProductType.COMPETITOR) {
            throw new ConflictException("ASIN is already registered as an own product");
        }

        if (competitorLinkRepository.findByOwnProductIdAndCompetitorProductId(
                ownProduct.getId(), competitor.getId()).isPresent()) {
            throw new ConflictException("Competitor link already exists");
        }

        CompetitorLink link = new CompetitorLink();
        link.setOwnProduct(ownProduct);
        link.setCompetitorProduct(competitor);
        competitorLinkRepository.save(link);
        return competitor;
    }

    @Transactional
    public AdminProductCatalogResponse updateCompetitor(Long linkId, String asin, Long newOwnProductId) {
        CompetitorLink link = getLinkWithProducts(linkId);
        Product newOwnProduct = productService.getProductEntity(newOwnProductId);
        if (newOwnProduct.getType() != ProductType.OWN) {
            throw new ConflictException("Competitors must be linked to an own product");
        }

        boolean asinChanged = !asin.trim().equalsIgnoreCase(link.getCompetitorProduct().getAsin());
        boolean ownProductChanged = !newOwnProductId.equals(link.getOwnProduct().getId());

        if (asinChanged) {
            productService.updateCompetitorProductAsin(link.getCompetitorProduct().getId(), asin);
            link = getLinkWithProducts(linkId);
        }

        if (ownProductChanged) {
            Long competitorProductId = link.getCompetitorProduct().getId();
            if (competitorLinkRepository.findByOwnProductIdAndCompetitorProductId(
                    newOwnProductId, competitorProductId).isPresent()) {
                throw new ConflictException("Competitor link already exists for target product");
            }
            link.setOwnProduct(newOwnProduct);
            competitorLinkRepository.save(link);
        }

        if (asinChanged || ownProductChanged) {
            productCacheService.evictAll();
            crawlService.crawlProductAsync(newOwnProductId);
        }

        return AdminProductCatalogResponse.fromCompetitorLink(getLinkWithProducts(linkId));
    }

    @Transactional
    public ProductResponse updateCompetitorAsin(Long linkId, String asin) {
        CompetitorLink link = getLinkWithProducts(linkId);
        Long ownProductId = link.getOwnProduct().getId();
        ProductResponse response = productService.updateCompetitorProductAsin(
                link.getCompetitorProduct().getId(), asin);
        crawlService.crawlProductAsync(ownProductId);
        return response;
    }

    @Transactional
    public AdminProductCatalogResponse updateCompetitorLink(Long linkId, Long newOwnProductId) {
        CompetitorLink link = getLinkWithProducts(linkId);
        Product newOwnProduct = productService.getProductEntity(newOwnProductId);
        if (newOwnProduct.getType() != ProductType.OWN) {
            throw new ConflictException("Competitors must be linked to an own product");
        }

        if (newOwnProductId.equals(link.getOwnProduct().getId())) {
            return AdminProductCatalogResponse.fromCompetitorLink(link);
        }

        Long competitorProductId = link.getCompetitorProduct().getId();
        if (competitorLinkRepository.findByOwnProductIdAndCompetitorProductId(
                newOwnProductId, competitorProductId).isPresent()) {
            throw new ConflictException("Competitor link already exists for target product");
        }

        link.setOwnProduct(newOwnProduct);
        competitorLinkRepository.save(link);
        productCacheService.evictAll();
        crawlService.crawlProductAsync(newOwnProductId);
        return AdminProductCatalogResponse.fromCompetitorLink(link);
    }

    private CompetitorLink getLinkWithProducts(Long linkId) {
        return competitorLinkRepository.findByIdWithProducts(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Competitor link not found: " + linkId));
    }

    @Transactional
    public void deleteCompetitorLink(Long linkId) {
        CompetitorLink link = competitorLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Competitor link not found: " + linkId));

        Long competitorId = link.getCompetitorProduct().getId();
        competitorLinkRepository.delete(link);
        competitorProductCleanup.deleteIfOrphaned(competitorId);
        productCacheService.evictAll();
    }
}
