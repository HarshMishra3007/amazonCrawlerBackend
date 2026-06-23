package com.amazon.productintelligence.crawler;

public class CrawlException extends RuntimeException {

    private final String reasonCode;

    public CrawlException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public CrawlException(String reasonCode, String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
