package com.amazon.productintelligence.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PriceHistoryPoint {
    Instant date;
    BigDecimal price;
}
