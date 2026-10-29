package com.retail.flow.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CartResponseDto {
    private Long cartId;
    private Long customerId;
    private Integer totalDistinctItems;
    private Integer totalQuantity;
    private BigDecimal estimatedTotalAmount;
    private LocalDateTime updatedAt;
    private List<CartItemResponseDto> items;

    @Data
    @Builder
    public static class CartItemResponseDto {
        private Long id;
        private Long productId;
        private String productName;
        private Long sellerId;
        private Long variantId;
        private String sku;
        private String size;
        private String color;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
        private Integer availableStock;
        private Integer minimumOrderQuantity;
        private Integer packSize;
        private Boolean productActive;
        private Boolean inStock;
    }
}