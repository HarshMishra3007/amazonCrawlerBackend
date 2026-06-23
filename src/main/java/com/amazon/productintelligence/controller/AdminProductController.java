package com.amazon.productintelligence.controller;

import com.amazon.productintelligence.dto.BulkAsinsRequest;
import com.amazon.productintelligence.dto.BulkImportResponse;
import com.amazon.productintelligence.dto.CreateProductRequest;
import com.amazon.productintelligence.dto.UpdateProductAsinRequest;
import com.amazon.productintelligence.dto.ProductResponse;
import com.amazon.productintelligence.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkImportResponse createProductsBulk(@Valid @RequestBody BulkAsinsRequest request) {
        return productService.createOwnProductsBulk(request.getAsins());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.createOwnProduct(request);
    }

    @PutMapping("/{id}/asin")
    public ProductResponse updateProductAsin(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductAsinRequest request) {
        return productService.updateOwnProductAsin(id, request.getAsin());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteOwnProduct(id);
    }
}
