package com.retail.flow.returns.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "sale_returns", indexes = {
        @Index(name = "idx_sale_returns_sale_id", columnList = "sale_id"),
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

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    private LocalDate returnDate;
    private String reason;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items;
}