package com.retail.flow.customer.repository;

import com.retail.flow.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Platform Buyer lookups (where sellerId IS NULL)
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.sellerId IS NULL")
    Optional<Customer> findByEmail(@Param("email") String email);

    boolean existsBySellerIdIsNullAndMobile(String mobile);

    // Seller-scoped offline Customer checks
    boolean existsBySellerIdAndMobile(Long sellerId, String mobile);

    boolean existsBySellerIdAndEmail(Long sellerId, String email);

    // BOLA Prevention: Checks if a Seller owns the Customer OR received an Order from them
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Customer c
            WHERE c.id = :customerId
              AND (c.sellerId = :sellerId
                   OR EXISTS (SELECT 1 FROM Order o WHERE o.customer = c AND o.sellerId = :sellerId))
            """)
    boolean isCustomerAccessibleBySeller(@Param("customerId") Long customerId, @Param("sellerId") Long sellerId);

    @Query("""
            SELECT DISTINCT c FROM Customer c
            WHERE c.sellerId = :sellerId
               OR EXISTS (SELECT 1 FROM Order o WHERE o.customer = c AND o.sellerId = :sellerId)
               OR EXISTS (SELECT 1 FROM Sale s WHERE s.customer = c AND s.sellerId = :sellerId)
            """)
    List<Customer> findCustomersForSeller(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COUNT(DISTINCT c.id) FROM Customer c
            WHERE c.sellerId = :sellerId
               OR EXISTS (SELECT 1 FROM Order o WHERE o.customer = c AND o.sellerId = :sellerId)
               OR EXISTS (SELECT 1 FROM Sale s WHERE s.customer = c AND s.sellerId = :sellerId)
            """)
    long countDistinctCustomersForSeller(@Param("sellerId") Long sellerId);
}