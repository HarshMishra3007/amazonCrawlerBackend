package com.amazon.productintelligence.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS_LIST = "productsList";
    public static final String PRODUCT_DETAIL = "productDetail";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(PRODUCTS_LIST, PRODUCT_DETAIL);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats());
        return manager;
    }
}
