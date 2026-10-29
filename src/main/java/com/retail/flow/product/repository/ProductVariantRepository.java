package com.retail.flow.product.repository;

import com.retail.flow.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Override
    @EntityGraph(attributePaths = {"product"})
    Optional<ProductVariant> findById(Long id);

    boolean existsBySellerIdAndSku(Long sellerId, String sku);

    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant v SET v.version = 0 WHERE v.version IS NULL")
    void fixNullVersions();

    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant v SET v.sellerId = (SELECT p.sellerId FROM Product p WHERE p.id = v.product.id) WHERE v.sellerId IS NULL OR v.sellerId = 0")
    void backfillNullSellerIds();
}