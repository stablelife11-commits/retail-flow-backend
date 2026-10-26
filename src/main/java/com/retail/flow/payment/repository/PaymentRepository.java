package com.retail.flow.payment.repository;

import com.retail.flow.payment.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"customer"})
    List<Payment> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"customer"})
    List<Payment> findBySellerId(Long sellerId);

    @Override
    @EntityGraph(attributePaths = {"customer"})
    List<Payment> findAll();
}