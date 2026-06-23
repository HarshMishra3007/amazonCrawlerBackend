package com.amazon.productintelligence.dto;

import com.amazon.productintelligence.model.CrawlStatus;
import com.amazon.productintelligence.model.ProductType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class CrawlStatusResponse {
    Long id;
    String asin;
    String name;
    ProductType type;
    Instant lastCrawlAt;
    CrawlStatus lastCrawlStatus;
    String lastCrawlError;
}
