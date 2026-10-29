package com.retail.flow.returns.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "sale_returns", indexes = {
        @Index(name = "idx_sale_returns_sale_id", columnList = "sale_id"),
        @Index(name = "idx_sale_returns_order_id", columnList = "order_id"),
        @Index(name = "idx_sale_returns_seller_id", columnList = "seller_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private LocalDate returnDate;

    private String reason;

    @Column(name = "total_return_amount", nullable = false, precision = 19, scale = 2, columnDefinition = "DECIMAL(19,2) NOT NULL DEFAULT 0.00")
    @Builder.Default
    private BigDecimal totalReturnAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ReturnItem> items;

    @PrePersist
    public void prePersist() {
        if (this.returnDate == null) {
            this.returnDate = LocalDate.now();
        }
        if (this.totalReturnAmount == null) {
            this.totalReturnAmount = BigDecimal.ZERO;
        }
    }
}