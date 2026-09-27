package com.retail.flow.sale.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SaleResponseDto {
    private Long id;
    private String saleNumber;
    private Long customerId;
    private String customerName;
    private LocalDate saleDate;
    private BigDecimal totalAmount;
    private List<SaleItemResponseDto> items;

    @Data
    @Builder
    public static class SaleItemResponseDto {
        private Long id;
        private Long variantId;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}