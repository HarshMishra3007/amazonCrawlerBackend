package com.amazon.productintelligence.config;

import com.amazon.productintelligence.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ProductCacheWarmup implements ApplicationRunner {

    private final ProductService productService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            var products = productService.listOwnProducts();
            products.forEach(product -> productService.getProductDetail(product.getId()));
            log.info("Warmed product cache for {} products", products.size());
        } catch (Exception ex) {
            log.warn("Product cache warmup skipped: {}", ex.getMessage());
        }
    }
}
