package com.retail.flow.purchase.repository;

import com.retail.flow.purchase.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p")
    BigDecimal sumTotalPurchases();

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p WHERE p.sellerId = :sellerId")
    BigDecimal sumTotalPurchasesBySellerId(@Param("sellerId") Long sellerId);

    @EntityGraph(attributePaths = {"supplier"})
    Page<Purchase> findBySellerId(Long sellerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"supplier"})
    Page<Purchase> findAll(Pageable pageable);
}