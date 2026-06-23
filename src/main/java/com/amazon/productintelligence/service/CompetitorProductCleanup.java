package com.amazon.productintelligence.service;

import com.amazon.productintelligence.repository.CompetitorLinkRepository;
import com.amazon.productintelligence.repository.CrawlSnapshotRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitorProductCleanup {

    private final CompetitorLinkRepository competitorLinkRepository;
    private final CrawlSnapshotRepository crawlSnapshotRepository;
    private final ProductRepository productRepository;

    public void deleteIfOrphaned(Long competitorProductId) {
        if (competitorLinkRepository.existsByCompetitorProductId(competitorProductId)) {
            return;
        }
        crawlSnapshotRepository.deleteByProductId(competitorProductId);
        productRepository.deleteById(competitorProductId);
    }

    public void deleteIfOrphaned(List<Long> competitorProductIds) {
        competitorProductIds.forEach(this::deleteIfOrphaned);
    }
}
