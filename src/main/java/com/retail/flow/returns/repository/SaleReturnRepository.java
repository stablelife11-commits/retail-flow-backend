package com.retail.flow.returns.repository;

import com.retail.flow.returns.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {
}