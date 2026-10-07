package com.retail.flow.returns.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ReturnRequestDto {
    @NotNull(message = "Sale ID is required")
    private Long saleId;

    private String reason;

    @NotEmpty(message = "Return items cannot be empty")
    @Valid // 🟢 NAYA: Make sure inner validations run
    private List<ReturnItemDto> items;

    @Data
    public static class ReturnItemDto {
        @NotNull(message = "Variant ID is required")
        private Long variantId;

        // 🟢 FIX: Ab koi negative value dekar stock nahi chura payega
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Return quantity must be at least 1")
        private Integer quantity;
    }
}