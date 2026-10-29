package com.retail.flow.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantRequestDto {

    // Optional: Used when updating an existing variant via PUT /api/v1/products/{id}
    private Long id;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String size;
    private String color;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Selling price must be greater than zero")
    private BigDecimal sellingPrice;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @Min(value = 1, message = "Minimum order quantity (MOQ) must be at least 1")
    private Integer minimumOrderQuantity = 1;

    @Min(value = 1, message = "Pack size must be at least 1")
    private Integer packSize = 1;

    private String hsnCode;

    @DecimalMin(value = "0.0", inclusive = true, message = "GST percentage cannot be negative")
    @DecimalMax(value = "28.0", inclusive = true, message = "GST percentage cannot exceed 28%")
    private BigDecimal gstPercentage = BigDecimal.ZERO;
}