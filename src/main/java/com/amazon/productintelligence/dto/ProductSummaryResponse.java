package com.amazon.productintelligence.dto;

import com.amazon.productintelligence.model.CrawlStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class ProductSummaryResponse {
    Long id;
    String asin;
    String name;
    BigDecimal currentPrice;
    String currency;
    Instant lastCrawlAt;
    CrawlStatus lastCrawlStatus;
}
