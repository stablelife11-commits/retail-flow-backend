package com.retail.flow.purchase.repository;

import com.retail.flow.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p")
    BigDecimal sumTotalPurchases();

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p WHERE p.sellerId = :sellerId")
    BigDecimal sumTotalPurchasesBySellerId(@Param("sellerId") Long sellerId);

    @EntityGraph(attributePaths = {"supplier", "purchaseItems", "purchaseItems.productVariant"})
    List<Purchase> findBySellerId(Long sellerId);

    @Override
    @EntityGraph(attributePaths = {"supplier", "purchaseItems", "purchaseItems.productVariant"})
    List<Purchase> findAll();
}