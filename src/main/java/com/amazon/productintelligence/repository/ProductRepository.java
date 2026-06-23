package com.amazon.productintelligence.repository;

import com.amazon.productintelligence.model.Product;
import com.amazon.productintelligence.model.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByAsin(String asin);

    boolean existsByAsin(String asin);

    List<Product> findByType(ProductType type);
}
