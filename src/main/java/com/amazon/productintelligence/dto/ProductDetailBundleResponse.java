package com.amazon.productintelligence.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProductDetailBundleResponse {
    ProductResponse product;
    List<PriceHistoryPoint> priceHistory;
    List<CompetitorComparisonResponse> competitors;
}
