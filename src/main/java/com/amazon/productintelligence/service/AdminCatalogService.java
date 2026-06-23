package com.amazon.productintelligence.service;

import com.amazon.productintelligence.dto.AdminProductCatalogResponse;
import com.amazon.productintelligence.model.ProductType;
import com.amazon.productintelligence.repository.CompetitorLinkRepository;
import com.amazon.productintelligence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final ProductRepository productRepository;
    private final CompetitorLinkRepository competitorLinkRepository;

    @Transactional(readOnly = true)
    public List<AdminProductCatalogResponse> getProductCatalog() {
        List<AdminProductCatalogResponse> catalog = new ArrayList<>();
        productRepository.findByType(ProductType.OWN).stream()
                .map(AdminProductCatalogResponse::fromOwnProduct)
                .forEach(catalog::add);
        competitorLinkRepository.findAllWithProducts().stream()
                .map(AdminProductCatalogResponse::fromCompetitorLink)
                .forEach(catalog::add);
        return catalog;
    }

    @Transactional(readOnly = true)
    public List<AdminProductCatalogResponse> getCompetitorLinksForOwnProduct(Long ownProductId) {
        return competitorLinkRepository.findAllWithProductsByOwnProductId(ownProductId).stream()
                .map(AdminProductCatalogResponse::fromCompetitorLink)
                .toList();
    }
}
