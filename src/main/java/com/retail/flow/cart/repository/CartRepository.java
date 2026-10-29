package com.retail.flow.cart.repository;

import com.retail.flow.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"customer", "items", "items.productVariant", "items.productVariant.product"})
    Optional<Cart> findByCustomerId(Long customerId);
}