package com.amazon.productintelligence.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BulkImportResponse {
    int succeeded;
    int failed;
    List<BulkImportItemResult> results;
}
