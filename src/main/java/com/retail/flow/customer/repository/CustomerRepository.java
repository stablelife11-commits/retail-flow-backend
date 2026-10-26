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
    Optional<Customer> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
    boolean existsByEmail(String email);
    Optional<Customer> findByEmail(String email);

    // SECURITY FIX (ISO-001): A Seller can only view customers they created OR customers who ordered/bought from them
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