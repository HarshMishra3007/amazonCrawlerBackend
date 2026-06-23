package com.amazon.productintelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProductAsinRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{10}$", message = "ASIN must be 10 alphanumeric characters")
    private String asin;
}
