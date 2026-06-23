package com.amazon.productintelligence.controller;

import com.amazon.productintelligence.dto.CompetitorComparisonResponse;
import com.amazon.productintelligence.dto.PriceHistoryPoint;
import com.amazon.productintelligence.dto.ProductDetailBundleResponse;
import com.amazon.productintelligence.dto.ProductResponse;
import com.amazon.productintelligence.dto.ProductSummaryResponse;
import com.amazon.productintelligence.service.CompetitorService;
import com.amazon.productintelligence.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CompetitorService competitorService;

    @GetMapping
    public List<ProductSummaryResponse> listOwnProducts() {
        return productService.listOwnProducts();
    }

    @GetMapping("/{id}/detail")
    public ProductDetailBundleResponse getProductDetail(@PathVariable Long id) {
        return productService.getProductDetail(id);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @GetMapping("/{id}/price-history")
    public List<PriceHistoryPoint> getPriceHistory(@PathVariable Long id) {
        return productService.getPriceHistory(id);
    }

    @GetMapping("/{id}/competitors")
    public List<CompetitorComparisonResponse> getCompetitors(@PathVariable Long id) {
        return competitorService.getCompetitorsForOwnProduct(id);
    }
}
