package com.amazon.productintelligence.crawler;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CrawlResult {
    String name;
    String description;
    BigDecimal price;
    String currency;
    String seller;
    List<String> images;
}
