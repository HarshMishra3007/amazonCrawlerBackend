package com.amazon.productintelligence.exception;

public class CrawlInProgressException extends RuntimeException {
    public CrawlInProgressException(String message) {
        super(message);
    }
}
