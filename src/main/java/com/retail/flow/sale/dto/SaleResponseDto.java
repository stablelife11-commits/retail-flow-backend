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
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal returnedAmount;
    private BigDecimal paidAmount;
    private String paymentStatus;
    private List<SaleItemResponseDto> items;

    @Data
    @Builder
    public static class SaleItemResponseDto {
        private Long id;
        private Long variantId;
        private String sku;
        private String hsnCode;
        private BigDecimal gstPercentage;
        private BigDecimal taxAmount;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}