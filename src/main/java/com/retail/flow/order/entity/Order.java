package com.retail.flow.order.entity;

import com.retail.flow.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_seller_id", columnList = "seller_id"),
        @Index(name = "idx_orders_customer_id", columnList = "customer_id")
})
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

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PLACED;

    @PrePersist
    public void prePersist() {
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.PLACED;
        }
    }

    public enum OrderStatus {
        PLACED,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REJECTED;

        public boolean canTransitionTo(OrderStatus target) {
            if (this == target) {
                return false;
            }
            return switch (this) {
                case PLACED -> Set.of(CONFIRMED, PROCESSING, REJECTED, CANCELLED).contains(target);
                case CONFIRMED -> Set.of(PROCESSING, SHIPPED, CANCELLED).contains(target);
                case PROCESSING -> Set.of(SHIPPED, CANCELLED).contains(target);
                case SHIPPED -> Set.of(DELIVERED).contains(target);
                case DELIVERED, CANCELLED, REJECTED -> false; // Terminal states
            };
        }

        public boolean isStockReleaseState() {
            return this == CANCELLED || this == REJECTED;
        }
    }
}