package com.retail.flow.returns.repository;

import com.retail.flow.returns.entity.SaleReturn;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    @EntityGraph(attributePaths = {"items"})
    List<SaleReturn> findBySaleId(Long saleId);
}