package com.retail.flow.inventory.entity;

import com.retail.flow.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // PURCHASE, SALE, ORDER_RESERVATION, ORDER_RELEASE, ADJUSTMENT, RETURN

    @Column(nullable = false)
    private Integer quantity;

    private String referenceType; // e.g., ORDER, PURCHASE, MANUAL
    private Long referenceId;     // ID of the order or purchase

    @Column(nullable = false)
    private Integer previousStock;

    @Column(nullable = false)
    private Integer newStock;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        PURCHASE,
        SALE,
        ORDER_RESERVATION,
        ORDER_RELEASE,
        ADJUSTMENT,
        RETURN
    }
}