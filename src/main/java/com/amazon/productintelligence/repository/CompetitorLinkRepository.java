package com.amazon.productintelligence.repository;

import com.amazon.productintelligence.model.CompetitorLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import java.util.Optional;

public interface CompetitorLinkRepository extends JpaRepository<CompetitorLink, Long> {

    List<CompetitorLink> findByOwnProductId(Long ownProductId);

    @Query("SELECT cl FROM CompetitorLink cl JOIN FETCH cl.competitorProduct WHERE cl.ownProduct.id = :ownProductId")
    List<CompetitorLink> findByOwnProductIdWithCompetitor(@Param("ownProductId") Long ownProductId);

    @Query("SELECT cl FROM CompetitorLink cl JOIN FETCH cl.ownProduct JOIN FETCH cl.competitorProduct")
    List<CompetitorLink> findAllWithProducts();

    @Query("""
            SELECT cl FROM CompetitorLink cl
            JOIN FETCH cl.ownProduct JOIN FETCH cl.competitorProduct
            WHERE cl.ownProduct.id = :ownProductId
            """)
    List<CompetitorLink> findAllWithProductsByOwnProductId(@Param("ownProductId") Long ownProductId);

    Optional<CompetitorLink> findByOwnProductIdAndCompetitorProductId(Long ownProductId, Long competitorProductId);

    @Query("""
            SELECT cl FROM CompetitorLink cl
            JOIN FETCH cl.ownProduct JOIN FETCH cl.competitorProduct
            WHERE cl.id = :id
            """)
    Optional<CompetitorLink> findByIdWithProducts(@Param("id") Long id);

    boolean existsByCompetitorProductId(Long competitorProductId);

    void deleteByOwnProductId(Long ownProductId);

    void deleteByCompetitorProductId(Long competitorProductId);
}
