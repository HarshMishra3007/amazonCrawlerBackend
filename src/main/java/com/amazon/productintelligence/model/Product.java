package com.amazon.productintelligence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String asin;

    @Column(length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "current_price", precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(length = 3)
    private String currency = "INR";

    private String seller;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "product_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductType type;

    @Column(name = "last_crawl_at")
    private Instant lastCrawlAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_crawl_status", columnDefinition = "crawl_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CrawlStatus lastCrawlStatus;

    @Column(name = "last_crawl_error", columnDefinition = "TEXT")
    private String lastCrawlError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
