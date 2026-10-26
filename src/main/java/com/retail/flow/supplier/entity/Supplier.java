package com.retail.flow.supplier.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"seller_id", "phone"})
}, indexes = {
        @Index(name = "idx_suppliers_seller_id", columnList = "seller_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    private String contactPerson;

    @Column(nullable = false)
    private String phone;

    private String email;

    private String address;
}