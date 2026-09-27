package com.retail.flow.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponseDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private List<OrderItemResponseDto> items;

    @Data
    @Builder
    public static class OrderItemResponseDto {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}