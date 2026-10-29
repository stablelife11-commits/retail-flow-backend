package com.retail.flow.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductResponseDto {
    private Long id;
    private Long sellerId;
    private String productCode;
    private String name;
    private String description;
    private String category;
    private String brand;
    private Boolean active;
    private List<VariantResponseDto> variants;
    private List<String> imageUrls;

    @Data
    @Builder
    public static class VariantResponseDto {
        private Long id;
        private String sku;
        private String size;
        private String color;
        private BigDecimal sellingPrice;

        // Excluded from JSON when null (hidden from buyers and non-owning sellers)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private BigDecimal purchasePrice;

        private Integer stock;
        private Integer minimumOrderQuantity;
        private Integer packSize;
        private String hsnCode;
        private BigDecimal gstPercentage;
    }
}