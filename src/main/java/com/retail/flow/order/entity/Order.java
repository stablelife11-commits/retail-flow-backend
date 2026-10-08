package com.retail.flow.order.entity;

import com.retail.flow.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    // 🟢 NAYA: Delivery Address (Customer app se aayega)
    private String deliveryAddress;

    // 🟢 NAYA: Order Status Tracker (PLACED, CONFIRMED, SHIPPED, DELIVERED)
    @Column(nullable = false)
    @Builder.Default
    private String status = "PLACED";

    @PrePersist
    public void prePersist() {
        this.orderDate = LocalDateTime.now();
    }
}