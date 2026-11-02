package com.retail.flow.inventory.repository;

import com.retail.flow.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByProductVariantId(Long productVariantId);

    void deleteByProductVariantId(Long productVariantId);

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    @Query("SELECT it FROM InventoryTransaction it WHERE it.productVariant.sellerId = :sellerId")
    Page<InventoryTransaction> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    Page<InventoryTransaction> findAll(Pageable pageable);
}