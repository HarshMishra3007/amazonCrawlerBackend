package com.amazon.productintelligence.dto;

import com.amazon.productintelligence.model.CompetitorLink;
import com.amazon.productintelligence.model.CrawlStatus;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.model.ProductType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazon.productintelligence.model.CrawlSnapshot;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductSummaryResponse toSummary(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .asin(product.getAsin())
                .name(product.getName())
                .currentPrice(product.getCurrentPrice())
                .currency(product.getCurrency())
                .lastCrawlAt(product.getLastCrawlAt())
                .lastCrawlStatus(product.getLastCrawlStatus())
                .build();
    }

    public static ProductResponse toResponse(Product product) {
        return toResponse(product, null);
    }

    public static ProductResponse toResponse(Product product, List<PriceHistoryPoint> priceHistory) {
        return ProductResponse.builder()
                .id(product.getId())
                .asin(product.getAsin())
                .name(product.getName())
                .description(product.getDescription())
                .currentPrice(effectivePrice(product.getCurrentPrice(), priceHistory))
                .currency(product.getCurrency())
                .seller(product.getSeller())
                .images(product.getImages() == null ? List.of() : product.getImages())
                .type(product.getType())
                .lastCrawlAt(product.getLastCrawlAt())
                .lastCrawlStatus(product.getLastCrawlStatus())
                .lastCrawlError(product.getLastCrawlError())
                .build();
    }

    public static CompetitorComparisonResponse toCompetitorComparison(
            Product ownProduct,
            List<PriceHistoryPoint> ownPriceHistory,
            CompetitorLink link,
            List<PriceHistoryPoint> priceHistory) {
        Product competitor = link.getCompetitorProduct();
        BigDecimal ownPrice = effectivePrice(ownProduct.getCurrentPrice(), ownPriceHistory);
        BigDecimal competitorPrice = effectivePrice(competitor.getCurrentPrice(), priceHistory);

        BigDecimal delta = null;
        if (ownPrice != null && competitorPrice != null) {
            delta = competitorPrice.subtract(ownPrice);
        }

        return CompetitorComparisonResponse.builder()
                .linkId(link.getId())
                .id(competitor.getId())
                .asin(competitor.getAsin())
                .name(competitor.getName())
                .currentPrice(competitorPrice)
                .currency(competitor.getCurrency())
                .priceDelta(delta)
                .priceHistory(priceHistory)
                .build();
    }

    public static BigDecimal effectivePrice(BigDecimal currentPrice, List<PriceHistoryPoint> priceHistory) {
        if (currentPrice != null) {
            return currentPrice;
        }
        if (priceHistory == null || priceHistory.isEmpty()) {
            return null;
        }
        return priceHistory.get(priceHistory.size() - 1).getPrice();
    }

    public static List<PriceHistoryPoint> toPriceHistory(List<CrawlSnapshot> snapshots) {
        return snapshots.stream()
                .map(snapshot -> PriceHistoryPoint.builder()
                        .date(snapshot.getCrawledAt())
                        .price(snapshot.getPrice())
                        .build())
                .toList();
    }

    public static Map<Long, List<PriceHistoryPoint>> groupPriceHistoryByProductId(List<CrawlSnapshot> snapshots) {
        return snapshots.stream()
                .collect(Collectors.groupingBy(
                        snapshot -> snapshot.getProduct().getId(),
                        Collectors.mapping(
                                snapshot -> PriceHistoryPoint.builder()
                                        .date(snapshot.getCrawledAt())
                                        .price(snapshot.getPrice())
                                        .build(),
                                Collectors.toList())));
    }

    public static CrawlStatusResponse toCrawlStatus(Product product) {
        return CrawlStatusResponse.builder()
                .id(product.getId())
                .asin(product.getAsin())
                .name(product.getName())
                .type(product.getType())
                .lastCrawlAt(product.getLastCrawlAt())
                .lastCrawlStatus(product.getLastCrawlStatus())
                .lastCrawlError(product.getLastCrawlError())
                .build();
    }

    public static void applyCrawlSuccess(Product product, com.amazon.productintelligence.crawler.CrawlResult result) {
        product.setName(result.getName());
        product.setDescription(result.getDescription());
        product.setCurrentPrice(result.getPrice());
        product.setCurrency(result.getCurrency());
        product.setSeller(result.getSeller());
        product.setImages(result.getImages() == null ? Collections.emptyList() : result.getImages());
        product.setLastCrawlStatus(CrawlStatus.SUCCESS);
        product.setLastCrawlError(null);
    }
}
