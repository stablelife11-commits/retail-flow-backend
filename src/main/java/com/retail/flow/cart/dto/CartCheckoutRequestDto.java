package com.retail.flow.cart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartCheckoutRequestDto {

    @NotBlank(message = "Delivery address is required for checkout")
    private String deliveryAddress;
}