package com.amazon.productintelligence.controller;

import com.amazon.productintelligence.dto.AdminProductCatalogResponse;
import com.amazon.productintelligence.dto.BulkCompetitorsRequest;
import com.amazon.productintelligence.dto.BulkImportResponse;
import com.amazon.productintelligence.dto.CreateCompetitorRequest;
import com.amazon.productintelligence.dto.UpdateCompetitorLinkRequest;
import com.amazon.productintelligence.dto.UpdateCompetitorRequest;
import com.amazon.productintelligence.dto.UpdateProductAsinRequest;
import com.amazon.productintelligence.dto.ProductResponse;
import com.amazon.productintelligence.service.AdminCatalogService;
import com.amazon.productintelligence.service.CompetitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/competitors")
@RequiredArgsConstructor
public class AdminCompetitorController {

    private final CompetitorService competitorService;
    private final AdminCatalogService adminCatalogService;

    @GetMapping
    public List<AdminProductCatalogResponse> listCompetitorLinks(@RequestParam Long ownProductId) {
        return adminCatalogService.getCompetitorLinksForOwnProduct(ownProductId);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkImportResponse createCompetitorsBulk(@Valid @RequestBody BulkCompetitorsRequest request) {
        return competitorService.createCompetitorsBulk(request.getOwnProductId(), request.getAsins());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createCompetitor(@Valid @RequestBody CreateCompetitorRequest request) {
        return competitorService.createCompetitor(request);
    }

    @PutMapping("/{linkId}")
    public AdminProductCatalogResponse updateCompetitor(
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateCompetitorRequest request) {
        return competitorService.updateCompetitor(linkId, request.getAsin(), request.getOwnProductId());
    }

    @PutMapping("/{linkId}/asin")
    public ProductResponse updateCompetitorAsin(
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateProductAsinRequest request) {
        return competitorService.updateCompetitorAsin(linkId, request.getAsin());
    }

    @PutMapping("/{linkId}/own-product")
    public AdminProductCatalogResponse updateCompetitorLink(
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateCompetitorLinkRequest request) {
        return competitorService.updateCompetitorLink(linkId, request.getOwnProductId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompetitorLink(@PathVariable Long id) {
        competitorService.deleteCompetitorLink(id);
    }
}
