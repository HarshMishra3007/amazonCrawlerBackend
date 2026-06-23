package com.amazon.productintelligence.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    private String schedule = "0 0 */6 * * *";
    private long delayMs = 2500;
    private long timeoutMs = 30000;
    private String amazonBaseUrl = "https://www.amazon.in";
    private String defaultCurrency = "INR";
    private String proxyUrl = "";
    private boolean dockerMode = false;
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private long delayMs = 3000;
        private double multiplier = 2.0;
    }
}
