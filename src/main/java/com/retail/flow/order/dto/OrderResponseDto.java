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
    private String customerMobile; // 🟢 NAYA: Seller ko dikhane ke liye
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private String status; // 🟢 NAYA
    private String deliveryAddress; // 🟢 NAYA
    private List<OrderItemResponseDto> items;

    @Data
    @Builder
    public static class OrderItemResponseDto {
        private Long productId;
        private String productName;
        private Long variantId; // 🟢 NAYA
        private String size; // 🟢 NAYA
        private String color; // 🟢 NAYA
        private Integer quantity;
        private BigDecimal price;
    }
}