package com.retail.flow.customer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_seller_id", columnList = "seller_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable for self-registered platform Buyers; set to seller's User ID when created by a Seller
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String mobile;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}