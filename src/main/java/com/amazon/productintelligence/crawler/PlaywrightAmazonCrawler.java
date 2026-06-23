package com.amazon.productintelligence.crawler;

import com.amazon.productintelligence.config.CrawlerProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightAmazonCrawler implements AmazonCrawler {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final Browser browser;
    private final CrawlerProperties crawlerProperties;
    private final PageParser pageParser;

    @Override
    @Retryable(
            retryFor = CrawlException.class,
            noRetryFor = BlockedCrawlException.class,
            maxAttemptsExpression = "${crawler.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${crawler.retry.delay-ms:3000}",
                    multiplierExpression = "${crawler.retry.multiplier:2.0}"
            ),
            listeners = "crawlRetryListener"
    )
    public CrawlResult crawl(String asin) {
        String url = crawlerProperties.getAmazonBaseUrl() + "/dp/" + asin;
        log.info("Crawling Amazon product ASIN={} url={}", asin, url);

        long start = System.currentTimeMillis();
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setViewportSize(1280, 720)
                .setLocale(localeForAmazonBaseUrl()));

        try (context) {
            Page page = context.newPage();
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(crawlerProperties.getTimeoutMs())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            page.waitForSelector("#productTitle, #title, h1",
                    new Page.WaitForSelectorOptions().setTimeout(crawlerProperties.getTimeoutMs()));

            CrawlResult result = pageParser.parse(page, asin);
            log.info("Crawl succeeded ASIN={} price={} durationMs={}",
                    asin, result.getPrice(), System.currentTimeMillis() - start);
            return result;
        } catch (CrawlException ex) {
            log.warn("Crawl failed ASIN={} reason={} message={}", asin, ex.getReasonCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("Crawl failed ASIN={} reason=UNKNOWN message={}", asin, ex.getMessage());
            throw new CrawlException("UNKNOWN", "Failed to crawl ASIN " + asin + ": " + ex.getMessage(), ex);
        }
    }

    @Recover
    public CrawlResult recover(CrawlException ex, String asin) {
        log.warn("Crawl exhausted all retry attempts ASIN={} reason={}", asin, ex.getReasonCode());
        throw ex;
    }

    private String localeForAmazonBaseUrl() {
        String baseUrl = crawlerProperties.getAmazonBaseUrl().toLowerCase();
        if (baseUrl.contains("amazon.in")) {
            return "en-IN";
        }
        if (baseUrl.contains("amazon.co.uk")) {
            return "en-GB";
        }
        return "en-US";
    }
}
