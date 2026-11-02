package com.retail.flow.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt hashed password

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // ADMIN, SELLER, BUYER

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "business_phone", length = 20)
    private String businessPhone;

    @Column(name = "business_address", length = 500)
    private String businessAddress;

    public enum Role {
        ADMIN,
        SELLER,
        BUYER
    }
}