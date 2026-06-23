package com.amazon.productintelligence.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BulkImportItemResult {
    String asin;
    boolean success;
    String message;
    Long productId;
}
