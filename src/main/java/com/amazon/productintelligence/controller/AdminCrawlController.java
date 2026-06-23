package com.amazon.productintelligence.controller;

import com.amazon.productintelligence.dto.AdminProductCatalogResponse;
import com.amazon.productintelligence.service.AdminCatalogService;
import com.amazon.productintelligence.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class AdminCrawlController {

    private final CrawlService crawlService;
    private final AdminCatalogService adminCatalogService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> triggerFullCrawl() {
        crawlService.crawlAllAsync();
        return Map.of("message", "Full crawl started");
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> triggerProductCrawl(@PathVariable Long productId) {
        crawlService.crawlProductAsync(productId);
        return Map.of("message", "Crawl started for product " + productId);
    }

    @GetMapping("/status")
    public List<AdminProductCatalogResponse> getCrawlStatus() {
        return adminCatalogService.getProductCatalog();
    }
}
