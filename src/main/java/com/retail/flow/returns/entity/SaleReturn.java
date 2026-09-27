package com.retail.flow.returns.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "sale_returns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long saleId;
    private LocalDate returnDate;
    private String reason;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items;
}