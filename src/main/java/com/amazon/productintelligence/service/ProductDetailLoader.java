package com.amazon.productintelligence.service;

import com.amazon.productintelligence.dto.PriceHistoryPoint;
import com.amazon.productintelligence.exception.ResourceNotFoundException;
import com.amazon.productintelligence.model.CompetitorLink;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.repository.CompetitorLinkRepository;
import com.amazon.productintelligence.repository.CrawlSnapshotRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductDetailLoader {

    private final ProductRepository productRepository;
    private final CrawlSnapshotRepository crawlSnapshotRepository;
    private final CompetitorLinkRepository competitorLinkRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Product loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<PriceHistoryPoint> loadHistory(Long id) {
        return crawlSnapshotRepository.findByProductIdOrderByCrawledAtAsc(id).stream()
                .map(snapshot -> PriceHistoryPoint.builder()
                        .date(snapshot.getCrawledAt())
                        .price(snapshot.getPrice())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<CompetitorLink> loadCompetitorLinks(Long ownProductId) {
        return competitorLinkRepository.findByOwnProductIdWithCompetitor(ownProductId);
    }
}
