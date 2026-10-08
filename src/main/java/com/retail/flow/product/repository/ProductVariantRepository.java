package com.retail.flow.product.repository;

import com.retail.flow.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph; // 🟢 NAYA IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // 🟢 PERFORMANCE FIX: Order place karte waqt Product ke N+1 queries rokne ke liye JOIN FETCH
    @Override
    @EntityGraph(attributePaths = {"product"})
    Optional<ProductVariant> findById(Long id);

    // 🟢 (Purana Fix) Database versions clean rakhne ke liye
    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant v SET v.version = 0 WHERE v.version IS NULL")
    void fixNullVersions();
}