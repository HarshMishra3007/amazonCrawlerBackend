package com.amazon.productintelligence.dto;

import com.amazon.productintelligence.model.CompetitorLink;
import com.amazon.productintelligence.model.CrawlStatus;
import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.model.ProductType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AdminProductCatalogResponse {
    Long id;
    Long linkId;
    Long ownProductId;
    String ownProductName;
    String asin;
    String name;
    ProductType type;
    Instant lastCrawlAt;
    CrawlStatus lastCrawlStatus;
    String lastCrawlError;

    public static AdminProductCatalogResponse fromOwnProduct(Product product) {
        return AdminProductCatalogResponse.builder()
                .id(product.getId())
                .asin(product.getAsin())
                .name(product.getName())
                .type(ProductType.OWN)
                .lastCrawlAt(product.getLastCrawlAt())
                .lastCrawlStatus(product.getLastCrawlStatus())
                .lastCrawlError(product.getLastCrawlError())
                .build();
    }

    public static AdminProductCatalogResponse fromCompetitorLink(CompetitorLink link) {
        Product competitor = link.getCompetitorProduct();
        Product ownProduct = link.getOwnProduct();
        return AdminProductCatalogResponse.builder()
                .id(competitor.getId())
                .linkId(link.getId())
                .ownProductId(ownProduct.getId())
                .ownProductName(ownProduct.getName() != null ? ownProduct.getName() : ownProduct.getAsin())
                .asin(competitor.getAsin())
                .name(competitor.getName())
                .type(ProductType.COMPETITOR)
                .lastCrawlAt(competitor.getLastCrawlAt())
                .lastCrawlStatus(competitor.getLastCrawlStatus())
                .lastCrawlError(competitor.getLastCrawlError())
                .build();
    }
}
