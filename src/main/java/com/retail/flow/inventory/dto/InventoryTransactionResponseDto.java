package com.retail.flow.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryTransactionResponseDto {
    private Long id;
    private Long variantId;
    private String sku;
    private String productName;
    private String size;
    private String color;
    private String type;
    private Integer quantity;
    private String referenceType;
    private Long referenceId;
    private Integer previousStock;
    private Integer newStock;
    private LocalDateTime createdAt;
}