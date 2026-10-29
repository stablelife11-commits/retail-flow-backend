package com.retail.flow.returns.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReturnRequestDto {

    // Provide either saleId (for offline POS sales) OR orderId (for delivered online orders)
    private Long saleId;

    private Long orderId;

    private String reason;

    @NotEmpty(message = "Return items cannot be empty")
    @Valid
    private List<ReturnItemDto> items;

    @Data
    public static class ReturnItemDto {
        @NotNull(message = "Variant ID is required")
        private Long variantId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Return quantity must be at least 1")
        private Integer quantity;
    }
}