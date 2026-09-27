package com.retail.flow.product.repository;

import com.retail.flow.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySellerIdAndProductCode(Long sellerId, String productCode);
}