package com.retail.flow.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaleRequestDto {
    @NotBlank(message = "Sale number is required")
    private String saleNumber;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Sale date is required")
    private LocalDate saleDate;

    @NotEmpty(message = "Sale must contain at least one item")
    @Valid
    private List<SaleItemRequestDto> items;
}