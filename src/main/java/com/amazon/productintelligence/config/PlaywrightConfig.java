package com.amazon.productintelligence.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PlaywrightConfig {

    private final CrawlerProperties crawlerProperties;

    private Playwright playwright;
    private Browser browser;

    @Bean
    public Playwright playwright() {
        playwright = Playwright.create();
        return playwright;
    }

    @Bean
    public Browser browser(Playwright playwrightInstance) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(true);

        if (StringUtils.hasText(crawlerProperties.getProxyUrl())) {
            options.setProxy(crawlerProperties.getProxyUrl());
        }

        if (crawlerProperties.isDockerMode()) {
            options.setArgs(dockerChromiumArgs());
        }

        browser = playwrightInstance.chromium().launch(options);
        log.info("Playwright Chromium browser started (dockerMode={})", crawlerProperties.isDockerMode());
        return browser;
    }

    private List<String> dockerChromiumArgs() {
        List<String> args = new ArrayList<>();
        args.add("--no-sandbox");
        args.add("--disable-setuid-sandbox");
        args.add("--disable-dev-shm-usage");
        args.add("--disable-gpu");
        return args;
    }

    @PreDestroy
    public void shutdown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        log.info("Playwright browser shut down");
    }
}
