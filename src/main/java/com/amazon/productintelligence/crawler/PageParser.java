package com.amazon.productintelligence.crawler;

import com.amazon.productintelligence.config.CrawlerProperties;
import com.microsoft.playwright.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile("[\\d,]+(?:\\.\\d{1,2})?");

    private final CrawlerProperties crawlerProperties;

    record ParsedPrice(BigDecimal amount, String currency) {
    }

    public CrawlResult parse(Page page, String asin) {
        detectBlocking(page);

        String name = extractName(page);
        String description = extractDescription(page);
        ParsedPrice parsedPrice = extractPrice(page);
        String seller = extractSeller(page);
        List<String> images = extractImages(page);

        if (parsedPrice == null || parsedPrice.amount() == null) {
            throw new CrawlException("PRICE_NOT_FOUND",
                    "Could not extract price for ASIN " + asin);
        }

        return CrawlResult.builder()
                .name(name)
                .description(description)
                .price(parsedPrice.amount())
                .currency(parsedPrice.currency())
                .seller(seller)
                .images(images)
                .build();
    }

    private void detectBlocking(Page page) {
        String title = page.title() == null ? "" : page.title().toLowerCase();
        String url = page.url() == null ? "" : page.url().toLowerCase();

        if (title.contains("robot check") || url.contains("validatecaptcha") || url.contains("/errors/")) {
            throw new BlockedCrawlException("Amazon blocked the request (CAPTCHA or robot check)");
        }
    }

    private String extractName(Page page) {
        return firstNonBlank(
                textContent(page, "#productTitle"),
                textContent(page, "span#title")
        );
    }

    private String extractDescription(Page page) {
        String bullets = textContent(page, "#feature-bullets");
        if (bullets != null) {
            return bullets;
        }
        return textContent(page, "#productDescription");
    }

    private ParsedPrice extractPrice(Page page) {
        String[] selectors = {
                ".a-price .a-offscreen",
                "#corePrice_feature_div .a-offscreen",
                "#corePriceDisplay_desktop_feature_div .a-offscreen",
                "#priceblock_ourprice",
                "#priceblock_dealprice",
                ".a-color-price"
        };

        for (String selector : selectors) {
            String raw = textContent(page, selector);
            ParsedPrice parsed = parsePriceRaw(raw);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    ParsedPrice parsePriceRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String currency = detectCurrency(raw);
        BigDecimal amount = parseNumericAmount(raw);
        if (amount == null) {
            return null;
        }
        return new ParsedPrice(amount, currency);
    }

    private String detectCurrency(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (raw.contains("₹") || lower.contains("inr") || lower.contains("rs.") || lower.contains("rs ")) {
            return "INR";
        }
        if (raw.contains("$") || lower.contains("usd")) {
            return "USD";
        }
        if (lower.contains("£") || lower.contains("gbp")) {
            return "GBP";
        }
        if (lower.contains("€") || lower.contains("eur")) {
            return "EUR";
        }
        return inferDefaultCurrency();
    }

    private String inferDefaultCurrency() {
        if (crawlerProperties.getDefaultCurrency() != null
                && !crawlerProperties.getDefaultCurrency().isBlank()) {
            return crawlerProperties.getDefaultCurrency().toUpperCase(Locale.ROOT);
        }

        String baseUrl = crawlerProperties.getAmazonBaseUrl().toLowerCase(Locale.ROOT);
        if (baseUrl.contains("amazon.in")) {
            return "INR";
        }
        if (baseUrl.contains("amazon.co.uk")) {
            return "GBP";
        }
        if (baseUrl.contains("amazon.de") || baseUrl.contains("amazon.fr") || baseUrl.contains("amazon.it")) {
            return "EUR";
        }
        return "INR";
    }

    private BigDecimal parseNumericAmount(String raw) {
        Matcher matcher = PRICE_PATTERN.matcher(raw.replace("$", "").replace("₹", "").replace("£", "").replace("€", ""));
        if (matcher.find()) {
            return new BigDecimal(matcher.group().replace(",", ""));
        }
        return null;
    }

    private String extractSeller(Page page) {
        return firstNonBlank(
                textContent(page, "#sellerProfileTriggerId"),
                textContent(page, "#merchant-info"),
                textContent(page, "#tabular-buybox .tabular-buybox-text")
        );
    }

    private List<String> extractImages(Page page) {
        Set<String> urls = new LinkedHashSet<>();

        addImageUrl(urls, page.locator("#landingImage").getAttribute("data-old-hires"));
        addImageUrl(urls, page.locator("#landingImage").getAttribute("src"));

        page.locator("#altImages img").all().forEach(el -> {
            addImageUrl(urls, el.getAttribute("data-old-hires"));
            addImageUrl(urls, el.getAttribute("src"));
        });

        if (urls.isEmpty()) {
            page.locator("img[data-old-hires]").all().forEach(el -> {
                addImageUrl(urls, el.getAttribute("data-old-hires"));
            });
        }

        return new ArrayList<>(urls);
    }

    private void addImageUrl(Set<String> urls, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String normalized = normalizeImageUrl(url);
        if (!normalized.contains("play-icon")) {
            urls.add(normalized);
        }
    }

    private String normalizeImageUrl(String url) {
        if (!url.contains("media-amazon.com")) {
            return url;
        }
        return url.replaceAll("\\._[A-Z]{2}[^.]*\\.", "._AC_SL1500_.");
    }

    private String textContent(Page page, String selector) {
        if (page.locator(selector).count() == 0) {
            return null;
        }
        String text = page.locator(selector).first().innerText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
