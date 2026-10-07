package com.retail.flow.order.repository;

import com.retail.flow.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 🟢 PERFORMANCE FIX: N+1 प्रॉब्लम रोकने के लिए JOINs का इस्तेमाल
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    List<Order> findBySellerId(Long sellerId);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    List<Order> findByCustomerId(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    List<Order> findAll();
}