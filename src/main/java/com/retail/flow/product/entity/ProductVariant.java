package com.retail.flow.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_variants_seller_sku", columnNames = {"seller_id", "sku"})
        },
        indexes = {
                @Index(name = "idx_product_variants_seller_id", columnList = "seller_id"),
                @Index(name = "idx_product_variants_product_id", columnList = "product_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Builder.Default
    private Long version = 0L;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String sku;

    private String size;

    private String color;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal sellingPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private Integer stock;

    // B2B Wholesale Garment Fields (Defaults to 1 for backward compatibility)
    @Column(name = "minimum_order_quantity", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 1")
    @Builder.Default
    private Integer minimumOrderQuantity = 1;

    @Column(name = "pack_size", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 1")
    @Builder.Default
    private Integer packSize = 1;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "gst_percentage", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0.00")
    @Builder.Default
    private BigDecimal gstPercentage = BigDecimal.ZERO;
}