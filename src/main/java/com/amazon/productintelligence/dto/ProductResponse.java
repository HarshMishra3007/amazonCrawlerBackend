package com.amazon.productintelligence.dto;

import com.amazon.productintelligence.model.CrawlStatus;
import com.amazon.productintelligence.model.ProductType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ProductResponse {
    Long id;
    String asin;
    String name;
    String description;
    BigDecimal currentPrice;
    String currency;
    String seller;
    List<String> images;
    ProductType type;
    Instant lastCrawlAt;
    CrawlStatus lastCrawlStatus;
    String lastCrawlError;
}
