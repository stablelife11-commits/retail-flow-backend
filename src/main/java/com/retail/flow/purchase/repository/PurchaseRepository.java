package com.retail.flow.purchase.repository;

import com.retail.flow.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    // 🟢 PERFORMANCE FIX: Direct SQL calculation
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p")
    BigDecimal sumTotalPurchases();
}