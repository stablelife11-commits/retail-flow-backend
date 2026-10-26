package com.retail.flow.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDto {
    // Optional: Ignored by backend in favor of JWT-authenticated buyer lookup
    private Long customerId;

    private String deliveryAddress;

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequestDto> items;
}