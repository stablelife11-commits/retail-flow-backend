package com.retail.flow.returns.repository;

import com.retail.flow.returns.entity.SaleReturn;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    @EntityGraph(attributePaths = {"items"})
    List<SaleReturn> findBySaleId(Long saleId);

    @EntityGraph(attributePaths = {"items"})
    List<SaleReturn> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"items"})
    List<SaleReturn> findBySellerId(Long sellerId);

    @Override
    @EntityGraph(attributePaths = {"items"})
    List<SaleReturn> findAll();

    @Query("SELECT COALESCE(SUM(sr.totalReturnAmount), 0) FROM SaleReturn sr")
    BigDecimal sumTotalReturns();

    @Query("SELECT COALESCE(SUM(sr.totalReturnAmount), 0) FROM SaleReturn sr WHERE sr.sellerId = :sellerId")
    BigDecimal sumTotalReturnsBySellerId(@Param("sellerId") Long sellerId);
}