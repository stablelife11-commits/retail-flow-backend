package com.retail.flow.product.repository;

import com.retail.flow.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // 🟢 NAYA IMPORT
import org.springframework.data.jpa.repository.Query; // 🟢 NAYA IMPORT
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // 🟢 NAYA IMPORT

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // 🟢 DATABASE FIX: Purane NULL versions ko 0 set karna taaki order crash na ho
    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant v SET v.version = 0 WHERE v.version IS NULL")
    void fixNullVersions();
}