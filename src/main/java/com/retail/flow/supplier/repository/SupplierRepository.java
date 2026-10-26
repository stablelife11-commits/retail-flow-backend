package com.retail.flow.supplier.repository;

import com.retail.flow.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findBySellerId(Long sellerId);
    long countBySellerId(Long sellerId);
    boolean existsBySellerIdAndPhone(Long sellerId, String phone);
}