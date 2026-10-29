package com.retail.flow.product.repository;

import com.retail.flow.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySellerIdAndProductCode(Long sellerId, String productCode);

    long countBySellerId(Long sellerId);

    Page<Product> findAll(Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND EXISTS (
                  SELECT 1 FROM User u
                  WHERE u.id = p.sellerId
                    AND u.active = true
                    AND u.role IN (com.retail.flow.user.entity.User$Role.SELLER, com.retail.flow.user.entity.User$Role.ADMIN)
              )
              AND (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
              AND (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Product> searchActiveMarketplaceProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("sellerId") Long sellerId,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
              AND (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Product> searchAllProductsForAdmin(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("sellerId") Long sellerId,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"variants"})
    Optional<Product> findById(Long id);
}