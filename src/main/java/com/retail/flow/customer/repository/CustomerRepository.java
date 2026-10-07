package com.retail.flow.customer.repository;

import com.retail.flow.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
    Optional<Customer> findByEmail(String email); // 🟢 SECURITY FIX: To find customer by JWT Email
}