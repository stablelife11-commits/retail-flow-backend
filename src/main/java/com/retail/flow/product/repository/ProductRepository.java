package com.retail.flow.product.repository;

import com.retail.flow.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySellerIdAndProductCode(Long sellerId, String productCode);

    // 🟢 FIX: Added Pageable for Pagination.
    // Note: @EntityGraph removed here to prevent Hibernate from doing dangerous "in-memory pagination".
    Page<Product> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"variants"})
    Optional<Product> findById(Long id);
}