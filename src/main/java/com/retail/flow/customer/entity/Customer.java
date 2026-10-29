package com.retail.flow.customer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customers_seller_mobile", columnNames = {"seller_id", "mobile"}),
                @UniqueConstraint(name = "uk_customers_seller_email", columnNames = {"seller_id", "email"})
        },
        indexes = {
                @Index(name = "idx_customers_seller_id", columnList = "seller_id"),
                @Index(name = "idx_customers_mobile", columnList = "mobile"),
                @Index(name = "idx_customers_email", columnList = "email")
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

    // NULL for self-registered platform Buyers; set to Seller's User ID when created in a Seller's ERP
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 10)
    private String mobile;

    @Column
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}