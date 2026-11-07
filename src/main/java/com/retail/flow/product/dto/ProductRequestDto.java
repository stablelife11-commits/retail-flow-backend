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

    // Optional: Agar blank bhejenge to backend Smart Product Code auto-generate kar dega
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