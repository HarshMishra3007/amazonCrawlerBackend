package com.amazon.productintelligence.repository;

import com.amazon.productintelligence.model.CrawlSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CrawlSnapshotRepository extends JpaRepository<CrawlSnapshot, Long> {

    List<CrawlSnapshot> findByProductIdOrderByCrawledAtAsc(Long productId);

    @Modifying
    @Query("DELETE FROM CrawlSnapshot cs WHERE cs.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Query("""
            SELECT cs FROM CrawlSnapshot cs
            JOIN FETCH cs.product
            WHERE cs.product.id IN :productIds
            ORDER BY cs.crawledAt ASC
            """)
    List<CrawlSnapshot> findByProductIdInOrderByCrawledAtAsc(@Param("productIds") List<Long> productIds);
}
