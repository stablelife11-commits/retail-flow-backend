package com.retail.flow.purchase.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PurchaseResponseDto {
    private Long id;
    private String purchaseNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDate purchaseDate;
    private BigDecimal totalAmount;
    private List<PurchaseItemResponseDto> items;

    @Data
    @Builder
    public static class PurchaseItemResponseDto {
        private Long id;
        private Long variantId;
        private String sku;
        private Integer quantity;
        private BigDecimal unitCost;
        private BigDecimal totalPrice;
    }
}