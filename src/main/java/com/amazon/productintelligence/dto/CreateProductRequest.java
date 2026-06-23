package com.amazon.productintelligence.dto;

import com.amazon.productintelligence.model.CrawlStatus;
import com.amazon.productintelligence.model.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateProductRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{10}$", message = "ASIN must be 10 alphanumeric characters")
    private String asin;
}
