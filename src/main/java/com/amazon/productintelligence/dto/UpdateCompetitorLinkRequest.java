package com.amazon.productintelligence.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCompetitorLinkRequest {

    @NotNull
    private Long ownProductId;
}
