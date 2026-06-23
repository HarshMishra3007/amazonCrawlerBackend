package com.amazon.productintelligence.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateCompetitorRequest {

    @NotNull
    @Pattern(regexp = "^[A-Z0-9]{10}$", message = "ASIN must be 10 alphanumeric characters")
    private String asin;

    @NotNull
    private Long ownProductId;
}
