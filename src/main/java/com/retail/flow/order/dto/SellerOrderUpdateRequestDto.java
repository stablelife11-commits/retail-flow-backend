package com.retail.flow.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SellerOrderUpdateRequestDto {

    private String deliveryAddress;

    private BigDecimal discountAmount;

    private String sellerRemarks;

    private Boolean confirmImmediately;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequestDto> items;
}