package com.retail.flow.returns.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReturnResponseDto {
    private Long id;
    private Long saleId;
    private Long orderId;
    private LocalDate returnDate;
    private String reason;
    private BigDecimal totalReturnAmount;
    private List<ReturnItemResponseDto> items;

    @Data
    @Builder
    public static class ReturnItemResponseDto {
        private Long id;
        private Long variantId;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}