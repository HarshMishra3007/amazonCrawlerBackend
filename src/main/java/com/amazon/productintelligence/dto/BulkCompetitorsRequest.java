package com.amazon.productintelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BulkCompetitorsRequest {

    @NotNull
    private Long ownProductId;

    @NotEmpty(message = "At least one ASIN is required")
    @Size(max = 50, message = "Maximum 50 ASINs per request")
    private List<
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{10}$", message = "ASIN must be 10 alphanumeric characters") String>
            asins;
}
