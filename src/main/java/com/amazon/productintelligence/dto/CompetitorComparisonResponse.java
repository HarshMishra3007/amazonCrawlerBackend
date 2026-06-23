package com.amazon.productintelligence.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CompetitorComparisonResponse {
    Long linkId;
    Long id;
    String asin;
    String name;
    BigDecimal currentPrice;
    String currency;
    BigDecimal priceDelta;
    List<PriceHistoryPoint> priceHistory;
}
