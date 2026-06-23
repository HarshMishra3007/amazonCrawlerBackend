package com.amazon.productintelligence.service;

import com.amazon.productintelligence.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class ProductCacheService {

    @CacheEvict(cacheNames = {CacheConfig.PRODUCTS_LIST, CacheConfig.PRODUCT_DETAIL}, allEntries = true)
    public void evictAll() {
        // Evicts product list and all detail entries after data changes.
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCT_DETAIL, key = "#productId")
    public void evictDetail(Long productId) {
        // Evicts a single product detail entry.
    }
}
