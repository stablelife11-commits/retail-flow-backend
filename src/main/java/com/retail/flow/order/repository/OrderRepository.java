package com.retail.flow.order.repository;

import com.retail.flow.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 🟢 FIX: Updated attribute paths to match ProductVariant
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.productVariant", "orderItems.productVariant.product"})
    List<Order> findBySellerId(Long sellerId);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.productVariant", "orderItems.productVariant.product"})
    List<Order> findByCustomerId(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.productVariant", "orderItems.productVariant.product"})
    List<Order> findAll();
}