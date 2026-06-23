package com.amazon.productintelligence.crawler;

public interface AmazonCrawler {
    CrawlResult crawl(String asin);
}
