package com.retail.flow.returns.dto;

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
    private List<ReturnItemDto> items;

    @Data
    public static class ReturnItemDto {
        @NotNull(message = "Variant ID is required")
        private Long variantId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }
}