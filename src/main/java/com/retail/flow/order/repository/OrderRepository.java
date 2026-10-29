package com.retail.flow.order.repository;

import com.retail.flow.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = com.retail.flow.order.entity.Order$OrderStatus.DELIVERED")
    BigDecimal sumDeliveredOrders();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.sellerId = :sellerId AND o.status = com.retail.flow.order.entity.Order$OrderStatus.DELIVERED")
    BigDecimal sumDeliveredOrdersBySellerId(@Param("sellerId") Long sellerId);

    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.productVariant", "orderItems.productVariant.product"})
    List<Order> findBySellerId(Long sellerId);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.productVariant", "orderItems.productVariant.product"})
    List<Order> findByCustomerId(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.productVariant", "orderItems.productVariant.product"})
    Optional<Order> findById(Long id);
}