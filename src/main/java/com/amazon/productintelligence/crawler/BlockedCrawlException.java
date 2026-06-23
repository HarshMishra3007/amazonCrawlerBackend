package com.amazon.productintelligence.crawler;

public class BlockedCrawlException extends CrawlException {

    public BlockedCrawlException(String message) {
        super("BLOCKED", message);
    }
}
