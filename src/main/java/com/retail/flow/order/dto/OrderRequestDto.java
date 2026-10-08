package com.retail.flow.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequestDto {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    // 🟢 NAYA: Customer checkout karte waqt address bhejega
    private String deliveryAddress;

    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequestDto> items;
}