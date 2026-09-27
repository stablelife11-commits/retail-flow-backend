package com.retail.flow.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductRequestDto {
    @NotNull(message = "Seller ID is required")
    private Long sellerId;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;
    private String category;
    private String brand;

    @NotEmpty(message = "Product must have at least one variant")
    @Valid
    private List<VariantRequestDto> variants;

    private List<String> imageUrls;
}