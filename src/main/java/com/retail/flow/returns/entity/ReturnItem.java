package com.retail.flow.returns.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "return_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "return_id")
    private SaleReturn saleReturn;

    private Long variantId;
    private String sku;
    private Integer quantity;
}