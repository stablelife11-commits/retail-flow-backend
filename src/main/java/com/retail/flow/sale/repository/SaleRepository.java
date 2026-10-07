package com.retail.flow.sale.repository;

import com.retail.flow.sale.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s")
    BigDecimal sumTotalSales();

    // 🟢 PERFORMANCE FIX: N+1 प्रॉब्लम रोकने के लिए
    @Override
    @EntityGraph(attributePaths = {"customer", "saleItems", "saleItems.productVariant"})
    List<Sale> findAll();
}