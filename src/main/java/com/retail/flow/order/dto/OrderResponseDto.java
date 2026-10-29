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
    private Long sellerId;
    private Long customerId;
    private String customerName;
    private String customerMobile;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String paymentStatus;
    private LocalDateTime orderDate;
    private String status;
    private String deliveryAddress;
    private List<OrderItemResponseDto> items;

    @Data
    @Builder
    public static class OrderItemResponseDto {
        private Long productId;
        private String productName;
        private Long variantId;
        private String size;
        private String color;
        private Integer quantity;
        private BigDecimal price;
    }
}