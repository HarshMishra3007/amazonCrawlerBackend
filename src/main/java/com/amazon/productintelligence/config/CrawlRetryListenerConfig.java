package com.amazon.productintelligence.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

@Slf4j
@Configuration
public class CrawlRetryListenerConfig {

    @Bean
    public RetryListenerSupport crawlRetryListener() {
        return new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("Crawl attempt {} failed: {}", context.getRetryCount(), throwable.getMessage());
            }
        };
    }
}
