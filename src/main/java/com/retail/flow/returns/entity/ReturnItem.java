package com.retail.flow.returns.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "return_items", indexes = {
        @Index(name = "idx_return_items_return_id", columnList = "return_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private SaleReturn saleReturn;

    @Column(nullable = false)
    private Long variantId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2, columnDefinition = "DECIMAL(19,2) NOT NULL DEFAULT 0.00")
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2, columnDefinition = "DECIMAL(19,2) NOT NULL DEFAULT 0.00")
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;
}